package com.qianxun.homestay.modules.agent.dto;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.Map;
/** 管理员对话请求。 */
public record AgentChatRequest(
    /** 续聊会话 ID，首次对话为空。 */ @Positive Long conversationId,
    /** 本轮用户消息。 */ @Size(max=10000, message="消息不能超过10000字") String message,
    /** 待答题卡所属的助手消息 ID。 */ @Positive Long questionMessageId,
    /** 以问题 ID 为键的答案。 */ Map<String, String> answers) {
    /** 兼容普通聊天调用。 */
    public AgentChatRequest(Long conversationId, String message) { this(conversationId, message, null, null); }
}
