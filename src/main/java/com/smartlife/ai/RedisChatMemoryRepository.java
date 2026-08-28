package com.smartlife.ai;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.smartlife.common.RedisConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 基于 Redis 的对话记忆仓储：多轮对话的上下文持久化在 Redis，
 * 服务无状态、可水平扩展，实例重启后会话记忆不丢失（高可用）。
 *
 * <p>每个会话一个 Key，消息序列化为 JSON 数组，24 小时滑动过期。</p>
 */
@Component
@RequiredArgsConstructor
public class RedisChatMemoryRepository implements ChatMemoryRepository {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public List<String> findConversationIds() {
        Set<String> keys = stringRedisTemplate.keys(RedisConstants.CHAT_MEMORY_KEY + "*");
        return keys.stream()
                .map(key -> key.substring(RedisConstants.CHAT_MEMORY_KEY.length()))
                .toList();
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        String json = stringRedisTemplate.opsForValue()
                .get(RedisConstants.CHAT_MEMORY_KEY + conversationId);
        if (json == null) {
            return List.of();
        }
        JSONArray array = JSONUtil.parseArray(json);
        List<Message> messages = new ArrayList<>(array.size());
        for (Object item : array) {
            JSONObject obj = (JSONObject) item;
            String type = obj.getStr("type");
            String text = obj.getStr("text", "");
            switch (type) {
                case "user" -> messages.add(new UserMessage(text));
                case "assistant" -> messages.add(new AssistantMessage(text));
                case "system" -> messages.add(new SystemMessage(text));
                default -> { /* 工具消息不做持久化 */ }
            }
        }
        return messages;
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        JSONArray array = new JSONArray();
        for (Message message : messages) {
            String type = message.getMessageType().getValue();
            if (!"user".equals(type) && !"assistant".equals(type) && !"system".equals(type)) {
                continue;
            }
            JSONObject obj = new JSONObject();
            obj.set("type", type);
            obj.set("text", message.getText());
            array.add(obj);
        }
        stringRedisTemplate.opsForValue().set(
                RedisConstants.CHAT_MEMORY_KEY + conversationId,
                array.toString(),
                Duration.ofHours(RedisConstants.CHAT_MEMORY_TTL_HOURS));
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        stringRedisTemplate.delete(RedisConstants.CHAT_MEMORY_KEY + conversationId);
    }
}
