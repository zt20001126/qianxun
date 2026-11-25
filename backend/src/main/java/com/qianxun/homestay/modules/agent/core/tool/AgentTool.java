package com.qianxun.homestay.modules.agent.core.tool;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
/** 智能体工具的定义、参数约束与执行边界。 */
public interface AgentTool {
    /** 模型使用的唯一工具名称。 */
    String name();
    /** 告知模型工具的用途。 */
    String description();
    /** 返回模型使用的参数约束。 */
    Map<String, Object> parameters();
    /** 校验参数并执行工具。 */
    AgentToolResult invoke(JsonNode arguments);
}
