package com.qianxun.homestay.modules.agent.core.model;
import com.qianxun.homestay.modules.agent.core.schema.*;
import java.util.List;
/** 智能体模型调用边界。 */
public interface AgentModel {
    /** 根据历史及系统提示词生成文本或工具调用。 */
    AgentModelResult generate(List<AgentModelMessage> messages);
}
