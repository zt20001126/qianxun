package com.qianxun.homestay.modules.agent.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
/** 管理员对话请求。 */
public record AgentChatRequest(
    /** 续聊会话 ID，首次对话为空。 */ @Positive Long conversationId,
    /** 本轮用户消息。 */ @NotBlank(message="消息不能为空") @Size(max=10000, message="消息不能超过10000字") String message) {}
