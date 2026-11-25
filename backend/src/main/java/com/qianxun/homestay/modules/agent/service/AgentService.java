package com.qianxun.homestay.modules.agent.service;
import com.qianxun.homestay.modules.agent.dto.AgentChatRequest;
import com.qianxun.homestay.modules.agent.vo.*;
/** 后台管理员对话服务。 */
public interface AgentService {
    /** 执行一轮对话并保存结果。 */ AgentChatVO chat(Long adminId, AgentChatRequest request);
    /** 分页查询管理员自己的会话。 */ AgentPageVO<AgentConversationVO> conversations(Long adminId, int page, int size);
    /** 分页查询管理员自己的会话消息。 */ AgentPageVO<AgentMessageVO> messages(Long adminId, Long conversationId, int page, int size);
}
