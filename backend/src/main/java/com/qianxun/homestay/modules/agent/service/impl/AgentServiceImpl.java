package com.qianxun.homestay.modules.agent.service.impl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.modules.agent.core.node.*;
import com.qianxun.homestay.modules.agent.core.schema.*;
import com.qianxun.homestay.modules.agent.dto.AgentChatRequest;
import com.qianxun.homestay.modules.agent.entity.*;
import com.qianxun.homestay.modules.agent.mapper.AgentMapper;
import com.qianxun.homestay.modules.agent.service.AgentService;
import com.qianxun.homestay.modules.agent.vo.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
/** 对话流程协调与会话持久化实现。 */
@Service public class AgentServiceImpl implements AgentService {
    private static final int MAX_HISTORY = 40;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int TITLE_LENGTH = 40;
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private final AgentMapper mapper;
    private final ObjectMapper json;
    private final TransactionTemplate transactions;
    private final InputNode input;
    private final ModelNode model;
    private final ToolNode tool;
    private final OutputNode output;
    public AgentServiceImpl(AgentMapper mapper, ObjectMapper json, TransactionTemplate transactions,
            InputNode input, ModelNode model, ToolNode tool, OutputNode output) {
        this.mapper=mapper; this.json=json; this.transactions=transactions; this.input=input; this.model=model; this.tool=tool; this.output=output;
    }
    @Override public AgentChatVO chat(Long adminId, AgentChatRequest request) {
        boolean answering = request.questionMessageId() != null || request.answers() != null;
        if (answering && (request.questionMessageId() == null || request.answers() == null
                || request.conversationId() == null || request.message() != null)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "答题请求格式不正确");
        }
        String text = answering ? null : input.execute(request.message()); Long id = request.conversationId();
        List<AgentMessageEntity> previous = List.of(); Long observed = null;
        if (id != null) {
            owned(id, adminId);
            previous = new ArrayList<>(mapper.recentMessages(id, MAX_HISTORY));
            Collections.reverse(previous);
            observed = previous.isEmpty() ? null : previous.get(previous.size()-1).id;
        }
        if (answering) {
            if (!Objects.equals(observed, request.questionMessageId()) || previous.isEmpty()) {
                throw new BusinessException(HttpStatus.CONFLICT, "题卡已失效或已经回答，请刷新会话");
            }
            text = validateAnswers(previous.get(previous.size() - 1), request.answers());
        }
        List<AgentModelMessage> history = previous.stream().map(this::toModelMessage).toList();
        // 网络调用不持有数据库锁；写入时通过会话行锁检测历史是否发生变化。
        AgentModelResult generated = model.execute(history, text);
        AgentChatVO result = generated.toolName() == null ? output.completed(generated) : output.waiting(tool.execute(generated));
        String userContent = answering
                ? encode(Map.of("parts", List.of(Map.of("type", "text", "text", text),
                        Map.of("type", "question_answers", "question_message_id", request.questionMessageId(), "answers", request.answers()))))
                : encode(Map.of("parts", List.of(Map.of("type", "text", "text", text))));
        String assistantContent = "completed".equals(result.status())
                ? encode(Map.of("parts", List.of(Map.of("type", "text", "text", result.answer()))))
                : encode(Map.of("parts", List.of(Map.of("type", "questions", "questions", result.questions()))));
        Long expected = observed;
        String submittedText = text;
        Long[] saved = transactions.execute(status -> {
            Long currentId = id;
            if (currentId == null) {
                AgentConversationEntity conversation = new AgentConversationEntity(); conversation.adminId = adminId;
                conversation.title = submittedText.substring(0, submittedText.offsetByCodePoints(0, Math.min(TITLE_LENGTH, submittedText.codePointCount(0, submittedText.length()))));
                mapper.insertConversation(conversation); currentId = conversation.id;
            } else {
                if (mapper.lockOwned(currentId, adminId) == null) throw notFound();
                if (!Objects.equals(expected, mapper.latestMessageId(currentId))) throw new BusinessException(HttpStatus.CONFLICT, "会话已更新，请重新发送消息");
            }
            save(currentId, "user", userContent); Long assistantId = save(currentId, "assistant", assistantContent); mapper.touchConversation(currentId);
            return new Long[]{currentId, assistantId};
        });
        return new AgentChatVO(saved[0], result.status(), result.answer(), result.questions(),
                "waiting".equals(result.status()) ? saved[1] : null);
    }
    @Override public AgentPageVO<AgentConversationVO> conversations(Long adminId, int page, int size) {
        int p = page(page); int s = size(size);
        var rows = mapper.listConversations(adminId, (long)(p-1)*s, s).stream().map(c -> new AgentConversationVO(c.id,c.title,time(c.createdAt),time(c.updatedAt))).toList();
        return new AgentPageVO<>(rows,mapper.countConversations(adminId),p,s);
    }
    @Override public AgentPageVO<AgentMessageVO> messages(Long adminId, Long id, int page, int size) {
        owned(id,adminId); int p=page(page); int s=size(size);
        var rows=mapper.listMessages(id,(long)(p-1)*s,s).stream().map(m -> new AgentMessageVO(m.id,m.role,parse(m.content),time(m.createdAt))).toList();
        return new AgentPageVO<>(rows,mapper.countMessages(id),p,s);
    }
    private AgentModelMessage toModelMessage(AgentMessageEntity m) {
        JsonNode parts = parse(m.content).path("parts"); List<String> text = new ArrayList<>();
        for (JsonNode part : parts) {
            if ("text".equals(part.path("type").asText())) text.add(part.path("text").asText());
            else if ("questions".equals(part.path("type").asText())) for (JsonNode q : part.path("questions")) {
                StringBuilder question = new StringBuilder(q.path("title").asText());
                if (q.path("context").isTextual() && !q.path("context").asText().isBlank()) question.append("\n").append(q.path("context").asText());
                for (JsonNode option : q.path("options")) question.append("\n- ").append(option.path("label").asText()).append("：").append(option.path("description").asText());
                text.add(question.toString());
            }
        }
        return new AgentModelMessage(m.role, String.join("\n", text));
    }
    private Long save(Long id,String role,String content) { AgentMessageEntity m=new AgentMessageEntity(); m.conversationId=id; m.role=role; m.content=content; mapper.insertMessage(m); return m.id; }
    /** 只允许回答本会话最新的助手题卡，避免历史题卡被重复提交。 */
    private String validateAnswers(AgentMessageEntity message, Map<String, String> answers) {
        if (!"assistant".equals(message.role)) throw new BusinessException(HttpStatus.CONFLICT, "题卡已失效或已经回答，请刷新会话");
        JsonNode parts = parse(message.content).path("parts");
        JsonNode questions = null;
        for (JsonNode part : parts) if ("questions".equals(part.path("type").asText())) questions = part.path("questions");
        if (questions == null || !questions.isArray() || questions.isEmpty() || answers.size() != questions.size()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "答案必须与题卡问题一一对应");
        }
        List<String> lines = new ArrayList<>();
        for (JsonNode question : questions) {
            String id = question.path("id").asText(); String answer = answers.get(id);
            if (answer == null || answer.isBlank() || answer.strip().length() > 100) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "请填写所有问题的有效答案");
            }
            answer = answer.strip();
            if ("single_choice".equals(question.path("type").asText())) {
                boolean valid = false;
                for (JsonNode option : question.path("options")) if (answer.equals(option.path("label").asText())) valid = true;
                if (!valid) throw new BusinessException(HttpStatus.BAD_REQUEST, "单选题答案不在选项中");
            }
            lines.add(question.path("title").asText() + "：" + answer);
        }
        return String.join("\n", lines);
    }
    private void owned(Long id,Long adminId) { if (mapper.findOwned(id,adminId)==null) throw notFound(); }
    private BusinessException notFound() { return new BusinessException(HttpStatus.NOT_FOUND,"会话不存在"); }
    private String encode(Object value) { try { return json.writeValueAsString(value); } catch (JsonProcessingException e) { throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR,"消息处理失败"); } }
    private JsonNode parse(String value) { try { return json.readTree(value); } catch (JsonProcessingException e) { throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR,"消息处理失败"); } }
    private int page(int p) { if (p<1) throw new BusinessException(HttpStatus.BAD_REQUEST,"页码必须大于零"); return p; }
    private int size(int s) { if (s<1 || s>MAX_PAGE_SIZE) throw new BusinessException(HttpStatus.BAD_REQUEST,"分页大小不正确"); return s; }
    private OffsetDateTime time(LocalDateTime value) { return value == null ? null : value.atZone(BUSINESS_ZONE).toOffsetDateTime(); }
}
