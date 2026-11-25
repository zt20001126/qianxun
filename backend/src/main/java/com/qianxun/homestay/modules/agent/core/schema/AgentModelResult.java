package com.qianxun.homestay.modules.agent.core.schema;
import com.fasterxml.jackson.databind.JsonNode;
/** 模型输出：普通文本或一个带关联标识的工具调用。 */
public record AgentModelResult(String text, String toolName, JsonNode toolArguments, String toolCallId) {
    public AgentModelResult(String text, String toolName, JsonNode toolArguments) {
        this(text, toolName, toolArguments, null);
    }
}
