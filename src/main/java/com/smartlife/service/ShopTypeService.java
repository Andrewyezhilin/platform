package com.smartlife.service;

import cn.hutool.json.JSONUtil;
import com.smartlife.common.RedisConstants;
import com.smartlife.entity.ShopType;
import com.smartlife.mapper.ShopTypeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * 商户类型服务：列表整体缓存于 Redis。
 */
@Service
@RequiredArgsConstructor
public class ShopTypeService {

    private final ShopTypeMapper shopTypeMapper;
    private final StringRedisTemplate stringRedisTemplate;

    public List<ShopType> queryList() {
        String json = stringRedisTemplate.opsForValue()
                .get(RedisConstants.CACHE_SHOP_TYPE_KEY);
        if (json != null) {
            return JSONUtil.toList(json, ShopType.class);
        }
        List<ShopType> list = shopTypeMapper.listAll();
        stringRedisTemplate.opsForValue().set(
                RedisConstants.CACHE_SHOP_TYPE_KEY,
                JSONUtil.toJsonStr(list),
                Duration.ofMinutes(RedisConstants.CACHE_SHOP_TYPE_TTL_MINUTES));
        return list;
    }
}
