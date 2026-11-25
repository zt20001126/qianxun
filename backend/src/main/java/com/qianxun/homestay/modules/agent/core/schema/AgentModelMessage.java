package com.qianxun.homestay.modules.agent.core.schema;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
/** 发送给模型的普通消息、工具调用或工具结果。 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AgentModelMessage(String role, String content,
        @JsonProperty("tool_calls") List<Map<String, Object>> toolCalls,
        @JsonProperty("tool_call_id") String toolCallId) {
    public AgentModelMessage(String role, String content) { this(role, content, null, null); }
    /** 保留工具调用标识及原始参数供模型继续推理。 */
    public static AgentModelMessage call(AgentModelResult result) {
        return new AgentModelMessage("assistant", null, List.of(Map.of("id", result.toolCallId(), "type", "function",
                "function", Map.of("name", result.toolName(), "arguments", result.toolArguments().toString()))), null);
    }
    /** 将执行结果关联到原始调用。 */
    public static AgentModelMessage tool(String callId, String content) {
        return new AgentModelMessage("tool", content, null, callId);
    }
}
