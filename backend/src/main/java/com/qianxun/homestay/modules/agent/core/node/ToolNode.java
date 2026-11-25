package com.qianxun.homestay.modules.agent.core.node;
import com.qianxun.homestay.modules.agent.core.schema.AgentModelResult;
import com.qianxun.homestay.modules.agent.core.tool.AgentToolRegistry;
import com.qianxun.homestay.modules.agent.core.tool.AgentToolResult;
import org.springframework.stereotype.Component;
/** 从注册表查找并执行模型请求的工具。 */
@Component
public class ToolNode {
    private final AgentToolRegistry registry;
    public ToolNode(AgentToolRegistry registry) { this.registry = registry; }
    /** 执行已注册工具并返回结果。 */
    public AgentToolResult execute(AgentModelResult result) {
        return registry.find(result.toolName()).invoke(result.toolArguments());
    }
}
