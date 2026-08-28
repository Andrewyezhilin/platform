package com.smartlife.service;

import com.smartlife.common.RedisConstants;
import com.smartlife.common.SystemConstants;
import com.smartlife.common.exception.BizException;
import com.smartlife.entity.Shop;
import com.smartlife.mapper.ShopMapper;
import com.smartlife.utils.CacheClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

/**
 * 商户服务：多级缓存查询 + Cache Aside 写一致性。
 */
@Service
@RequiredArgsConstructor
public class ShopService {

    private final ShopMapper shopMapper;
    private final CacheClient cacheClient;

    /**
     * 商户详情：Caffeine(L1) -> Redis(L2) -> 布隆过滤器 -> 互斥回源 MySQL。
     */
    public Shop queryById(Long id) {
        return cacheClient.queryWithMultiLevelCache(
                RedisConstants.CACHE_SHOP_KEY, id, Shop.class,
                shopMapper::selectById,
                Duration.ofMinutes(RedisConstants.CACHE_SHOP_TTL_MINUTES));
    }

    /**
     * Cache Aside 写路径：先更新数据库，再删除缓存（含集群 L1 失效广播）。
     * 二者置于同一事务边界内，DB 失败时不触碰缓存。
     */
    @Transactional
    public void update(Shop shop) {
        if (shop.getId() == null) {
            throw new BizException("商户 ID 不能为空");
        }
        shopMapper.update(shop);
        cacheClient.evict(RedisConstants.CACHE_SHOP_KEY + shop.getId());
    }

    public List<Shop> queryByType(Long typeId, Integer page) {
        int current = page == null || page < 1 ? 1 : page;
        return shopMapper.selectByTypeId(typeId,
                (current - 1) * SystemConstants.DEFAULT_PAGE_SIZE,
                SystemConstants.DEFAULT_PAGE_SIZE);
    }

    public List<Shop> queryByName(String name, Integer page) {
        int current = page == null || page < 1 ? 1 : page;
        return shopMapper.selectByName(name == null ? "" : name,
                (current - 1) * SystemConstants.DEFAULT_PAGE_SIZE,
                SystemConstants.DEFAULT_PAGE_SIZE);
    }
}
