CREATE TABLE agent_conversation (
 id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '会话主键',
 admin_id BIGINT UNSIGNED NOT NULL COMMENT '所属管理员',
 title VARCHAR(255) NULL COMMENT '会话标题',
 created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间，北京时间',
 updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最近更新时间，北京时间',
 PRIMARY KEY (id), KEY idx_agent_conversation_admin_updated (admin_id, updated_at),
 CONSTRAINT fk_agent_conversation_admin FOREIGN KEY (admin_id) REFERENCES admin(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='管理员智能体会话';
CREATE TABLE agent_message (
 id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '消息主键',
 conversation_id BIGINT UNSIGNED NOT NULL COMMENT '所属会话',
 role VARCHAR(20) NOT NULL COMMENT '消息角色 user 或 assistant',
 content JSON NOT NULL COMMENT '文本或自由文本提问内容',
 created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间，北京时间',
 PRIMARY KEY (id), KEY idx_agent_message_conversation_id (conversation_id, id),
 CONSTRAINT ck_agent_message_role CHECK (role IN ('user','assistant')),
 CONSTRAINT fk_agent_message_conversation FOREIGN KEY (conversation_id) REFERENCES agent_conversation(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='智能体会话消息';
