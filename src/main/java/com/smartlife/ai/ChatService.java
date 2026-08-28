package com.smartlife.ai;

import cn.hutool.core.util.IdUtil;
import com.smartlife.common.UserHolder;
import com.smartlife.dto.UserDTO;
import com.smartlife.entity.Blog;
import com.smartlife.service.BlogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * MindBridge 智能客服服务。
 *
 * <p>多轮对话：以 conversationId 关联 Redis 中的会话记忆；
 * 个性化：登录用户的 userId 通过 ToolContext 传递给工具层，
 * 支持"查我的订单"等个性化指令。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatClient chatClient;
    private final BlogService blogService;

    /**
     * 多轮对话。conversationId 为空时创建新会话。
     *
     * @return [conversationId, 回复内容]
     */
    public Map<String, String> chat(String conversationId, String message) {
        UserDTO user = UserHolder.getUser();
        // 会话与用户绑定，避免越权读取他人上下文
        String cid = (conversationId == null || conversationId.isBlank())
                ? user.getId() + ":" + IdUtil.simpleUUID()
                : conversationId;

        Map<String, Object> toolContext = new HashMap<>();
        toolContext.put("userId", user.getId());

        String reply = chatClient.prompt()
                .user(message)
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, cid))
                .toolContext(toolContext)
                .call()
                .content();

        Map<String, String> result = new HashMap<>();
        result.put("conversationId", cid);
        result.put("reply", reply);
        return result;
    }

    /**
     * 商户评论智能分析：汇总近期用户评论，输出情感倾向、优缺点与改进建议。
     */
    public String analyzeShopReviews(Long shopId) {
        var blogs = blogService.queryByShopId(shopId, 30);
        if (blogs.isEmpty()) {
            return "该商户暂无用户评论，无法分析。";
        }
        String reviews = blogs.stream()
                .map(Blog::getContent)
                .collect(Collectors.joining("\n---\n"));
        return chatClient.prompt()
                .user(u -> u.text("""
                        请对以下商户的用户评论进行分析，输出：
                        1. 总体情感倾向（好评/中评/差评占比估计）；
                        2. 用户提到最多的优点（最多 3 条）；
                        3. 用户提到最多的问题（最多 3 条）；
                        4. 给商户的改进建议。
                        评论内容：
                        {reviews}
                        """).param("reviews", reviews))
                .call()
                .content();
    }
}
