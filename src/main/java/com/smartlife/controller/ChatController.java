package com.smartlife.controller;

import com.smartlife.ai.ChatService;
import com.smartlife.common.Result;
import com.smartlife.dto.ChatRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * MindBridge 智能客服接口。
 */
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /** 多轮对话：携带 conversationId 维持上下文记忆 */
    @PostMapping("/chat")
    public Result chat(@Valid @RequestBody ChatRequest request) {
        return Result.ok(chatService.chat(request.getConversationId(), request.getMessage()));
    }

    /** 商户评论智能分析：情感倾向 + 优缺点总结 + 改进建议 */
    @GetMapping("/review-analysis/{shopId}")
    public Result analyzeReviews(@PathVariable("shopId") Long shopId) {
        return Result.ok(chatService.analyzeShopReviews(shopId));
    }
}
