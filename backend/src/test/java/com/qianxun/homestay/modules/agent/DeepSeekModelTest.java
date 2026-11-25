package com.qianxun.homestay.modules.agent;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qianxun.homestay.modules.agent.core.model.DeepSeekModel;
import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.modules.agent.core.schema.AgentModelMessage;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
/** 使用本地 HTTP 服务验证 DeepSeek 请求协议。 */
class DeepSeekModelTest {
    @Test void reportsInsufficientProviderBalanceWithoutExposingResponse() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/chat/completions", exchange -> {
            byte[] response = "private upstream details".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(402, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            var model = new DeepSeekModel("http://127.0.0.1:" + server.getAddress().getPort(),
                    "test-key", "deepseek-flash", 60, new ObjectMapper());
            var error = assertThrows(BusinessException.class,
                    () -> model.generate(List.of(new AgentModelMessage("user", "你好"))));
            assertEquals("模型服务账户余额不足，请联系管理员充值", error.getMessage());
        } finally {
            server.stop(0);
        }
    }
    @Test void sendsToolsAndExplicitlyDisablesThinking() throws Exception {
        ObjectMapper json=new ObjectMapper(); AtomicReference<String> captured=new AtomicReference<>();
        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/chat/completions",exchange -> {
            captured.set(new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));
            byte[] response="{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"content\":\"你好\"}}]}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200,response.length); exchange.getResponseBody().write(response); exchange.close();
        }); server.start();
        try {
            var model=new DeepSeekModel("http://127.0.0.1:"+server.getAddress().getPort(),"test-key","deepseek-flash",60,json);
            assertEquals("你好",model.generate(List.of(new AgentModelMessage("user","你好"))).text());
            var request=json.readTree(captured.get()); assertEquals("disabled",request.path("thinking").path("type").asText());
            assertEquals("ask_user_questions",request.path("tools").get(0).path("function").path("name").asText());
        } finally { server.stop(0); }
    }
}
