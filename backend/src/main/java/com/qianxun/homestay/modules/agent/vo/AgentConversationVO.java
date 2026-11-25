package com.qianxun.homestay.modules.agent.vo;
import java.time.OffsetDateTime;
/** 管理员会话列表项，时间按北京时间存储。 */
public record AgentConversationVO(/** 会话 ID。 */ Long id, /** 会话标题。 */ String title,
    /** 创建时间。 */ OffsetDateTime createdAt, /** 最近更新时间。 */ OffsetDateTime updatedAt) {}
