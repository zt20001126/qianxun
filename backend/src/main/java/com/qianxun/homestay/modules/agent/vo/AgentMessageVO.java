package com.qianxun.homestay.modules.agent.vo;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.OffsetDateTime;
/** 用户可见的历史消息，时间按北京时间存储。 */
public record AgentMessageVO(/** 消息 ID。 */ Long id, /** 消息角色。 */ String role,
    /** 消息内容及题卡。 */ JsonNode content, /** 创建时间。 */ OffsetDateTime createdAt) {}
