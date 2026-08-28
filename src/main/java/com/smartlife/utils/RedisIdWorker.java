package com.smartlife.utils;

import com.smartlife.common.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * 基于 Redis 的全局唯一 ID 生成器。
 *
 * <p>结构：1 位符号位 + 31 位时间戳（秒） + 32 位当日序列号，趋势递增、可按天分片统计。</p>
 */
@Component
public class RedisIdWorker {

    /** 起始纪元：2025-01-01 00:00:00 UTC */
    private static final long BEGIN_TIMESTAMP = 1735689600L;
    private static final int COUNT_BITS = 32;

    private final StringRedisTemplate stringRedisTemplate;

    public RedisIdWorker(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public long nextId(String keyPrefix) {
        long timestamp = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) - BEGIN_TIMESTAMP;
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        Long count = stringRedisTemplate.opsForValue()
                .increment(RedisConstants.ICR_KEY + keyPrefix + ":" + date);
        return (timestamp << COUNT_BITS) | (count == null ? 0 : count);
    }
}
