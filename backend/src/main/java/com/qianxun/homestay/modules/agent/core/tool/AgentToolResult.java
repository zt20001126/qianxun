package com.qianxun.homestay.modules.agent.core.tool;
import com.qianxun.homestay.modules.agent.vo.AgentQuestionVO;
import java.util.List;
/** 工具执行结果：继续交给模型的内容或等待用户回答的题卡。 */
public record AgentToolResult(String content, List<AgentQuestionVO> questions) {
    /** 创建可回传模型的结果。 */
    public static AgentToolResult completed(String content) { return new AgentToolResult(content, null); }
    /** 创建等待用户回答的题卡。 */
    public static AgentToolResult waiting(List<AgentQuestionVO> questions) { return new AgentToolResult(null, questions); }
}
