package com.qianxun.homestay.modules.agent.vo;
import java.util.List;
/** 单轮对话结果。 */
public record AgentChatVO(/** 会话 ID。 */ Long conversationId, /** completed 或 waiting。 */ String status,
    /** 最终回答。 */ String answer, /** 待回答问题。 */ List<AgentQuestionVO> questions,
    /** 待答题卡消息 ID。 */ Long questionMessageId) {
    /** 创建尚未持久化的对话结果。 */
    public AgentChatVO(Long conversationId, String status, String answer, List<AgentQuestionVO> questions) {
        this(conversationId, status, answer, questions, null);
    }
}
