package com.qianxun.homestay.modules.agent.core.schema;
/** 发送给模型的对话消息。 */
public record AgentModelMessage(String role, String content) {}
