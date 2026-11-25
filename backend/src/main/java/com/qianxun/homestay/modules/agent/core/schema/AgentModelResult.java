package com.qianxun.homestay.modules.agent.core.schema;
import com.fasterxml.jackson.databind.JsonNode;
/** 模型输出：普通文本或一个工具调用。 */
public record AgentModelResult(String text, String toolName, JsonNode toolArguments) {}
