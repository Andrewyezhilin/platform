package com.smartlife.config;

import com.smartlife.common.RedisConstants;
import com.smartlife.utils.CacheClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.nio.charset.StandardCharsets;

/**
 * 集群 L1 缓存失效订阅：任一节点执行 Cache Aside 删除时，
 * 通过 Redis Pub/Sub 通知所有节点同步失效各自的 Caffeine 本地缓存，
 * 避免多实例间读到过期的 L1 数据。
 */
@Slf4j
@Configuration
public class CacheEvictSubscriber {

    @Bean
    public RedisMessageListenerContainer cacheEvictListenerContainer(
            RedisConnectionFactory connectionFactory, CacheClient cacheClient) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener((message, pattern) -> {
            String key = new String(message.getBody(), StandardCharsets.UTF_8);
            log.debug("收到 L1 缓存失效广播: {}", key);
            cacheClient.evictLocal(key);
        }, new ChannelTopic(RedisConstants.TOPIC_CACHE_EVICT));
        return container;
    }
}
