package com.smartlife.utils;

import cn.hutool.json.JSONUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.smartlife.common.RedisConstants;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

/**
 * 多级缓存客户端：Caffeine(L1) + Redis(L2)，毫秒级响应。
 *
 * <p>缓存三大问题的治理策略：</p>
 * <ul>
 *   <li><b>穿透</b>：Redisson 布隆过滤器前置拦截不存在的 ID，空值短 TTL 缓存兜底；</li>
 *   <li><b>雪崩</b>：写入 Redis 时在基础 TTL 上叠加随机抖动（动态 TTL），避免同批 Key 同时失效；</li>
 *   <li><b>击穿</b>：热点 Key 重建时使用 Redis 互斥锁，只放行一个线程回源数据库。</li>
 * </ul>
 *
 * <p>一致性策略：<b>Cache Aside</b> —— 读时旁路加载，写时"先更新数据库、再删除缓存"，
 * 并通过 Redis Pub/Sub 广播使集群内所有节点的 L1 本地缓存同步失效。</p>
 */
@Slf4j
@Component
public class CacheClient {

    /** L1 中的空值占位符，避免缓存穿透打到 L2/DB */
    private static final String NULL_PLACEHOLDER = "\u0000NULL\u0000";
    /** TTL 随机抖动比例上限（20%） */
    private static final double TTL_JITTER_RATIO = 0.2;
    private static final long LOCK_TTL_SECONDS = 10L;

    private final StringRedisTemplate stringRedisTemplate;
    private final Cache<String, String> localCache;
    private final RedissonClient redissonClient;

    public CacheClient(StringRedisTemplate stringRedisTemplate,
                       Cache<String, String> localCache,
                       RedissonClient redissonClient) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.localCache = localCache;
        this.redissonClient = redissonClient;
    }

    /**
     * 多级缓存查询：L1 -> L2 -> 布隆过滤器 -> 互斥重建回源 DB。
     *
     * @param keyPrefix  缓存 Key 前缀
     * @param id         业务 ID
     * @param type       返回类型
     * @param dbFallback 回源函数
     * @param baseTtl    L2 基础 TTL（写入时叠加随机抖动）
     */
    public <R, ID> R queryWithMultiLevelCache(String keyPrefix, ID id, Class<R> type,
                                              Function<ID, R> dbFallback, Duration baseTtl) {
        String key = keyPrefix + id;

        // 1. L1 本地缓存（Caffeine），命中直接返回，纳秒级
        String local = localCache.getIfPresent(key);
        if (local != null) {
            return NULL_PLACEHOLDER.equals(local) ? null : JSONUtil.toBean(local, type);
        }

        // 2. L2 Redis 缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        if (json != null) {
            if (json.isEmpty()) {
                // 空值缓存命中（穿透兜底）
                localCache.put(key, NULL_PLACEHOLDER);
                return null;
            }
            localCache.put(key, json);
            return JSONUtil.toBean(json, type);
        }

        // 3. 布隆过滤器：确定不存在的 ID 直接拒绝，防止恶意穿透
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter(RedisConstants.BLOOM_SHOP_KEY);
        if (RedisConstants.CACHE_SHOP_KEY.equals(keyPrefix)
                && bloomFilter.isExists() && !bloomFilter.contains(String.valueOf(id))) {
            log.debug("布隆过滤器拦截不存在的 Key: {}", key);
            return null;
        }

        // 4. 互斥锁重建缓存（防击穿：仅一个线程回源）
        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
        String lockValue = UUID.randomUUID().toString();
        boolean locked = tryLock(lockKey, lockValue);
        try {
            if (!locked) {
                // 未抢到锁：短暂自旋后重查缓存
                Thread.sleep(50);
                return queryWithMultiLevelCache(keyPrefix, id, type, dbFallback, baseTtl);
            }
            // Double Check：拿到锁后再查一次，避免重复回源
            json = stringRedisTemplate.opsForValue().get(key);
            if (json != null) {
                return json.isEmpty() ? null : JSONUtil.toBean(json, type);
            }

            R r = dbFallback.apply(id);
            if (r == null) {
                // 空值缓存，短 TTL 兜底防穿透
                stringRedisTemplate.opsForValue().set(key, "",
                        Duration.ofMinutes(RedisConstants.CACHE_NULL_TTL_MINUTES));
                localCache.put(key, NULL_PLACEHOLDER);
                return null;
            }
            String value = JSONUtil.toJsonStr(r);
            set(key, value, baseTtl);
            return r;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("缓存重建被中断", e);
        } finally {
            if (locked) {
                unlock(lockKey, lockValue);
            }
        }
    }

    /**
     * 写入 L2（动态 TTL：基础值 + 最多 20% 的随机抖动，防雪崩）并同步 L1。
     */
    public void set(String key, String value, Duration baseTtl) {
        long jitterMillis = (long) (baseTtl.toMillis() * TTL_JITTER_RATIO
                * ThreadLocalRandom.current().nextDouble());
        stringRedisTemplate.opsForValue().set(key, value, baseTtl.plusMillis(jitterMillis));
        localCache.put(key, value);
    }

    /**
     * Cache Aside 写路径：删除 L2 与本节点 L1，并广播其余节点失效 L1。
     * 调用方需保证"先更新数据库、后调用本方法"。
     */
    public void evict(String key) {
        stringRedisTemplate.delete(key);
        localCache.invalidate(key);
        stringRedisTemplate.convertAndSend(RedisConstants.TOPIC_CACHE_EVICT, key);
    }

    /**
     * 收到集群失效广播时，仅清理本地 L1。
     */
    public void evictLocal(String key) {
        localCache.invalidate(key);
    }

    private boolean tryLock(String key, String value) {
        Boolean ok = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, value, Duration.ofSeconds(LOCK_TTL_SECONDS));
        return Boolean.TRUE.equals(ok);
    }

    private void unlock(String key, String value) {
        // 校验持有者，避免误删他人锁
        String current = stringRedisTemplate.opsForValue().get(key);
        if (value.equals(current)) {
            stringRedisTemplate.delete(key);
        }
    }
}
