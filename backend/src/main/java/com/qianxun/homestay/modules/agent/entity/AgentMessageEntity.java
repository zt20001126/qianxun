package com.qianxun.homestay.modules.agent.entity;
import java.time.LocalDateTime;
/** 会话消息表映射，数据库时间为北京时间。 */
public class AgentMessageEntity {
    /** 消息主键。 */ public Long id;
    /** 会话 ID。 */ public Long conversationId;
    /** 消息角色。 */ public String role;
    /** JSON 消息内容。 */ public String content;
    /** 创建时间。 */ public LocalDateTime createdAt;
}
