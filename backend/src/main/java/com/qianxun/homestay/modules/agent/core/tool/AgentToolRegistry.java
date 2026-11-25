package com.qianxun.homestay.modules.agent.core.tool;
import com.qianxun.homestay.common.exception.BusinessException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
/** 自动收集智能体工具，并统一提供模型定义和执行查找。 */
@Component
public class AgentToolRegistry {
    private final Map<String, AgentTool> tools = new LinkedHashMap<>();
    public AgentToolRegistry(List<AgentTool> registered) {
        for (AgentTool tool : registered) {
            if (tool.name() == null || tool.name().isBlank() || tools.putIfAbsent(tool.name(), tool) != null) {
                throw new IllegalStateException("智能体工具名称为空或重复");
            }
        }
    }
    /** 返回模型使用的工具定义。 */
    public List<Map<String, Object>> definitions() {
        return tools.values().stream().map(tool -> Map.<String, Object>of("type", "function",
                "function", Map.of("name", tool.name(), "description", tool.description(), "parameters", tool.parameters()))).toList();
    }
    /** 按名称查找允许执行的工具。 */
    public AgentTool find(String name) {
        AgentTool tool = tools.get(name);
        if (tool == null) throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "模型请求了不支持的工具");
        return tool;
    }
}
