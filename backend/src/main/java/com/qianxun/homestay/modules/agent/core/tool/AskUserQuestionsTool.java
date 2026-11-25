package com.qianxun.homestay.modules.agent.core.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.modules.agent.vo.AgentQuestionOptionVO;
import com.qianxun.homestay.modules.agent.vo.AgentQuestionVO;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** 校验并生成自由文本题和四选一题卡。 */
@Component
public class AskUserQuestionsTool {
    private static final int MAX_QUESTIONS = 3;
    private static final int OPTION_COUNT = 4;
    private static final int MAX_TITLE = 100;
    private static final int MAX_DESCRIPTION = 240;
    private static final int MAX_CONTEXT = 3000;
    /** 将模型参数转换为经过校验的问题列表。 */
    public List<AgentQuestionVO> execute(JsonNode arguments) {
        if (arguments == null || !arguments.isObject() || arguments.size() != 1) throw invalid();
        JsonNode questions = arguments.path("questions");
        if (!questions.isArray() || questions.isEmpty() || questions.size() > MAX_QUESTIONS) throw invalid();
        List<AgentQuestionVO> result = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (JsonNode q : questions) {
            if (!q.isObject() || !q.path("id").isTextual() || !q.path("title").isTextual()) throw invalid();
            String id = q.path("id").asText();
            String title = singleLine(q.path("title"), MAX_TITLE);
            if (!id.matches("[a-z][a-z0-9_]{0,63}") || !ids.add(id)) throw invalid();
            String type = q.has("type") ? q.path("type").asText() : "text";
            if ("text".equals(type)) {
                if (q.size() != (q.has("type") ? 3 : 2)) throw invalid();
                result.add(new AgentQuestionVO(id, type, title));
            } else if ("single_choice".equals(type)) {
                Set<String> allowed = Set.of("id", "type", "title", "context", "options", "allow_custom_input");
                q.fieldNames().forEachRemaining(name -> { if (!allowed.contains(name)) throw invalid(); });
                if (!q.path("allow_custom_input").isBoolean() || q.path("allow_custom_input").asBoolean()) throw invalid();
                JsonNode options = q.path("options");
                if (!options.isArray() || options.size() != OPTION_COUNT) throw invalid();
                String context = null;
                if (q.hasNonNull("context")) {
                    if (!q.path("context").isTextual() || q.path("context").asText().length() > MAX_CONTEXT) throw invalid();
                    context = q.path("context").asText().strip();
                }
                Set<String> labels = new HashSet<>();
                List<AgentQuestionOptionVO> choices = new ArrayList<>();
                for (JsonNode option : options) {
                    if (!option.isObject() || option.size() != 2) throw invalid();
                    String label = singleLine(option.path("label"), MAX_TITLE);
                    String description = singleLine(option.path("description"), MAX_DESCRIPTION);
                    if (!labels.add(label.toLowerCase(Locale.ROOT))) throw invalid();
                    choices.add(new AgentQuestionOptionVO(label, description));
                }
                result.add(new AgentQuestionVO(id, type, title, context, List.copyOf(choices), false));
            } else throw invalid();
        }
        return List.copyOf(result);
    }
    private String singleLine(JsonNode value, int limit) {
        if (!value.isTextual()) throw invalid();
        String text = value.asText().strip();
        if (text.isEmpty() || text.length() > limit || text.contains("\n") || text.contains("\r")) throw invalid();
        return text;
    }
    private BusinessException invalid() { return new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "模型生成的问题格式无效"); }
}
