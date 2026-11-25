package com.qianxun.homestay.modules.agent.core.tool;
import com.fasterxml.jackson.databind.JsonNode;
import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.modules.agent.vo.AgentQuestionVO;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
/** 仅校验并生成自由文本提问，不访问外部服务。 */
@Component public class AskUserQuestionsTool {
    /** 将模型参数转换为经过校验的问题列表。 */
    public List<AgentQuestionVO> execute(JsonNode arguments) {
        if (arguments == null || !arguments.isObject() || arguments.size() != 1) throw invalid();
        JsonNode questions = arguments.path("questions");
        if (!questions.isArray() || questions.size() < 1 || questions.size() > 3) throw invalid();
        List<AgentQuestionVO> result = new ArrayList<>(); var ids = new HashSet<String>();
        for (JsonNode q : questions) {
            if (!q.isObject() || q.size() != 2 || !q.path("id").isTextual() || !q.path("title").isTextual()) throw invalid();
            String id = q.path("id").asText(); String title = q.path("title").asText().strip();
            if (!id.matches("[a-z][a-z0-9_]{0,63}") || !ids.add(id) || title.isBlank() || title.length() > 100 || title.contains("\n") || title.contains("\r")) throw invalid();
            result.add(new AgentQuestionVO(id, "text", title));
        }
        return List.copyOf(result);
    }
    private BusinessException invalid() { return new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "模型生成的问题格式无效"); }
}
