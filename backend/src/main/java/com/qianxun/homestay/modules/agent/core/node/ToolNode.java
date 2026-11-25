package com.qianxun.homestay.modules.agent.core.node;
import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.modules.agent.core.schema.AgentModelResult;
import com.qianxun.homestay.modules.agent.core.tool.AskUserQuestionsTool;
import com.qianxun.homestay.modules.agent.vo.AgentQuestionVO;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
/** 检查模型工具名称并执行提问工具，结果交给输出节点。 */
@Component public class ToolNode {
    private final AskUserQuestionsTool tool;
    public ToolNode(AskUserQuestionsTool tool) { this.tool = tool; }
    /** 只允许执行已注册的提问工具。 */ public List<AgentQuestionVO> execute(AgentModelResult result) {
        if (!"ask_user_questions".equals(result.toolName())) throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "模型请求了不支持的工具");
        return tool.execute(result.toolArguments());
    }
}
