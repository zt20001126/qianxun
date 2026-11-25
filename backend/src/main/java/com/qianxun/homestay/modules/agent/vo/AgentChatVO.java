package com.qianxun.homestay.modules.agent.vo;
import java.util.List;
/** 单轮对话结果。 */
public record AgentChatVO(/** 会话 ID。 */ Long conversationId, /** completed 或 waiting。 */ String status,
    /** 最终回答。 */ String answer, /** 待回答问题。 */ List<AgentQuestionVO> questions) {}
