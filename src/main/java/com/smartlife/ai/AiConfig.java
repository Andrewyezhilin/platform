package com.smartlife.ai;

import com.smartlife.ai.mcp.SmartLifeTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MindBridge 智能客服配置。
 *
 * <ul>
 *   <li>ChatMemory：滑动窗口保留最近 20 条消息，仓储层落 Redis，支持多轮对话与上下文记忆；</li>
 *   <li>ChatClient：挂载记忆 Advisor 与平台工具，模型可自主调用工具完成业务查询；</li>
 *   <li>ToolCallbackProvider：把同一套工具注册到 MCP Server，对外提供标准化 AI 服务。</li>
 * </ul>
 */
@Configuration
public class AiConfig {

    private static final String SYSTEM_PROMPT = """
            你是"智慧生活服务平台"的智能客服 MindBridge，友好、专业、简洁。
            你可以帮助用户：搜索商户、查看商户详情与营业信息、查询优惠券与秒杀活动、
            查询用户本人的订单、分析商户的用户评论并给出总结。
            规则：
            1. 优先调用工具获取真实数据，不要编造商户、价格或订单信息；
            2. 涉及用户个人订单时，如果用户未登录，引导用户先登录；
            3. 金额单位为分时请换算成元展示；
            4. 回答使用简体中文。
            """;

    @Bean
    public ChatMemory chatMemory(RedisChatMemoryRepository repository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(20)
                .build();
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder,
                                 ChatMemory chatMemory,
                                 SmartLifeTools smartLifeTools) {
        return builder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultTools(smartLifeTools)
                .build();
    }

    /**
     * 注册 MCP 工具：MCP Server Starter 会自动发现该 Provider，
     * 通过 SSE 端点把工具暴露给任意标准 MCP 客户端（Claude、Cursor 等）。
     */
    @Bean
    public ToolCallbackProvider smartLifeToolCallbackProvider(SmartLifeTools smartLifeTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(smartLifeTools)
                .build();
    }
}
