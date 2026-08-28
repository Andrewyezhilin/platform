package com.smartlife.config;

import com.smartlife.common.RedisConstants;
import com.smartlife.mapper.ShopMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 商户布隆过滤器初始化：启动时把全量商户 ID 灌入布隆过滤器，
 * 之后不存在的商户 ID 在缓存层即被拦截，防止缓存穿透。
 *
 * <p>预期容量 100 万、误判率 1%；新增商户时由业务侧同步 add。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BloomFilterInitializer implements ApplicationRunner {

    private final RedissonClient redissonClient;
    private final ShopMapper shopMapper;

    @Override
    public void run(ApplicationArguments args) {
        try {
            RBloomFilter<String> bloomFilter =
                    redissonClient.getBloomFilter(RedisConstants.BLOOM_SHOP_KEY);
            bloomFilter.tryInit(1_000_000L, 0.01);
            List<Long> ids = shopMapper.listAllIds();
            ids.forEach(id -> bloomFilter.add(String.valueOf(id)));
            log.info("商户布隆过滤器初始化完成，共加载 {} 个商户 ID", ids.size());
        } catch (Exception e) {
            log.warn("布隆过滤器初始化失败（不影响启动，将退化为空值缓存防穿透）: {}", e.getMessage());
        }
    }
}
