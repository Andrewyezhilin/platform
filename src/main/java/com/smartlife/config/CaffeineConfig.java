package com.smartlife.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * L1 本地缓存配置（Caffeine）。
 *
 * <p>L1 只做热点数据的短周期兜底（默认 30s），容量受限并采用 W-TinyLFU 淘汰，
 * 命中时纳秒级返回；数据的权威副本在 Redis(L2) 与 MySQL。</p>
 */
@Configuration
public class CaffeineConfig {

    @Bean
    public Cache<String, String> localCache(
            @Value("${smartlife.cache.local.maximum-size:10000}") long maximumSize,
            @Value("${smartlife.cache.local.expire-seconds:30}") long expireSeconds) {
        return Caffeine.newBuilder()
                .maximumSize(maximumSize)
                .expireAfterWrite(Duration.ofSeconds(expireSeconds))
                .recordStats()
                .build();
    }
}
