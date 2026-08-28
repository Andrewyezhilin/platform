package com.smartlife.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 智能客服对话请求。
 */
@Data
public class ChatRequest {

    /** 会话 ID，为空则由服务端生成，客户端后续携带以维持多轮上下文 */
    private String conversationId;

    @NotBlank(message = "消息内容不能为空")
    private String message;
}
