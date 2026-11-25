package com.qianxun.homestay.modules.agent.core.node;
import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.modules.agent.core.schema.AgentModelResult;
import com.qianxun.homestay.modules.agent.vo.*;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
/** 输出节点：将模型文本或提问列表整理为稳定结果。 */
@Component public class OutputNode {
    /** 检查模型文本并生成完成结果。 */ public AgentChatVO completed(AgentModelResult result) {
        if (result.text() == null || result.text().isBlank()) throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "模型未生成有效回答");
        return new AgentChatVO(null, "completed", result.text().strip(), null);
    }
    /** 提问结果结束本轮并等待用户回答。 */ public AgentChatVO waiting(List<AgentQuestionVO> questions) {
        return new AgentChatVO(null, "waiting", null, questions);
    }
}
