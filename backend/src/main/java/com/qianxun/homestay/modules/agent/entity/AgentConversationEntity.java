package com.qianxun.homestay.modules.agent.entity;
import java.time.LocalDateTime;
/** 会话表映射，数据库时间为北京时间。 */
public class AgentConversationEntity {
    /** 会话主键。 */ public Long id;
    /** 管理员 ID。 */ public Long adminId;
    /** 会话标题。 */ public String title;
    /** 创建时间。 */ public LocalDateTime createdAt;
    /** 更新时间。 */ public LocalDateTime updatedAt;
}
