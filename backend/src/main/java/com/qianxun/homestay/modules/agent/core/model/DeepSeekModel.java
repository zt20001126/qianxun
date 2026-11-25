package com.qianxun.homestay.modules.agent.core.model;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.modules.agent.core.schema.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
/** DeepSeek Chat Completions 适配器；密钥由环境变量注入，不记录对话及密钥。 */
@Component
public class DeepSeekModel implements AgentModel {
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final ObjectMapper mapper;
    private final HttpClient client;
    private final Duration timeout;
    public DeepSeekModel(@Value("${app.agent.base-url}") String baseUrl, @Value("${app.agent.api-key}") String apiKey,
                         @Value("${app.agent.model}") String model,
                         @Value("${app.agent.timeout-seconds:60}") long timeoutSeconds, ObjectMapper mapper) {
        this.baseUrl = baseUrl; this.apiKey = apiKey; this.model = model; this.mapper = mapper;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        this.client = HttpClient.newBuilder().connectTimeout(timeout).build();
    }
    @Override public AgentModelResult generate(List<AgentModelMessage> messages) {
        if (apiKey.isBlank() || baseUrl.isBlank() || model.isBlank()) throw failure("智能体模型配置不完整");
        // 工具参数包含自由文本题和四选一题；服务端仍需校验模型输出。
        var option = Map.of("type", "object", "properties", Map.of("label", Map.of("type", "string"), "description", Map.of("type", "string")),
                "required", List.of("label", "description"), "additionalProperties", false);
        var textQuestion = Map.of("type", "object", "properties", Map.of("id", Map.of("type", "string"), "title", Map.of("type", "string"), "type", Map.of("type", "string", "enum", List.of("text"))),
                "required", List.of("id", "title", "type"), "additionalProperties", false);
        var choiceQuestion = Map.of("type", "object", "properties", Map.of("id", Map.of("type", "string"), "title", Map.of("type", "string"),
                "type", Map.of("type", "string", "enum", List.of("single_choice")), "context", Map.of("type", "string"),
                "options", Map.of("type", "array", "minItems", 4, "maxItems", 4, "items", option),
                "allow_custom_input", Map.of("type", "boolean", "enum", List.of(false))),
                "required", List.of("id", "title", "type", "options", "allow_custom_input"), "additionalProperties", false);
        var parameters = Map.of("type", "object", "properties", Map.of("questions", Map.of("type", "array", "minItems", 1, "maxItems", 3,
                "items", Map.of("anyOf", List.of(textQuestion, choiceQuestion)))), "required", List.of("questions"), "additionalProperties", false);
        var tool = Map.of("type", "function", "function", Map.of("name", "ask_user_questions", "description", "在缺少关键信息时提出自由文本题或四选一题", "parameters", parameters));
        var payload = Map.of("model", model, "messages", messages, "thinking", Map.of("type", "disabled"), "tools", List.of(tool), "stream", false);
        try {
            String url = baseUrl.replaceAll("/+$", "") + "/chat/completions";
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(timeout)
                    .header("Authorization", "Bearer " + apiKey).header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload))).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            // 上游余额不足需要账户管理员处理，仍作为服务不可用返回，避免暴露上游响应。
            if (response.statusCode() == HttpStatus.PAYMENT_REQUIRED.value()) {
                throw failure("模型服务账户余额不足，请联系管理员充值");
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) throw failure("模型服务暂时不可用");
            JsonNode root = mapper.readTree(response.body());
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.size() != 1) throw failure("模型响应格式无效");
            JsonNode choice = choices.get(0); JsonNode message = choice.path("message");
            String reason = choice.path("finish_reason").asText(); JsonNode calls = message.path("tool_calls");
            if ("tool_calls".equals(reason)) {
                if (!calls.isArray() || calls.size() != 1) throw failure("模型工具调用格式无效");
                JsonNode function = calls.get(0).path("function");
                return new AgentModelResult(null, function.path("name").asText(), mapper.readTree(function.path("arguments").asText()));
            }
            if (!"stop".equals(reason) || !message.path("content").isTextual() || message.path("content").asText().isBlank()) throw failure("模型未返回完整回答");
            return new AgentModelResult(message.path("content").asText(), null, null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); throw failure("模型请求已中断");
        } catch (java.io.IOException | IllegalArgumentException e) { throw failure("模型服务请求失败"); }
    }
    private BusinessException failure(String message) { return new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, message); }
}
