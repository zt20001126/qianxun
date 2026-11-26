package com.qianxun.homestay.modules.agent;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qianxun.homestay.common.security.service.JwtService;
import com.qianxun.homestay.modules.agent.core.model.AgentModel;
import com.qianxun.homestay.modules.agent.core.schema.AgentModelResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
/** 使用 MySQL 验证迁移、身份隔离及消息持久化。 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties="app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
@AutoConfigureMockMvc
class AgentApiIntegrationTest {
    @Container static final MySQLContainer<?> MYSQL=new MySQLContainer<>("mysql:8.0").withDatabaseName("agent_test").withUsername("qianxun").withPassword("test");
    @DynamicPropertySource static void db(DynamicPropertyRegistry r) { r.add("spring.datasource.url",MYSQL::getJdbcUrl); r.add("spring.datasource.username",MYSQL::getUsername); r.add("spring.datasource.password",MYSQL::getPassword); }
    @Autowired MockMvc mvc; @Autowired JdbcTemplate jdbc; @Autowired JwtService jwt; @Autowired ObjectMapper json;
    @MockitoBean AgentModel model;
    @Test void validatesAndPersistsStructuredAnswers() throws Exception {
        jdbc.update("INSERT INTO admin(username,password_hash,enabled) VALUES ('agent_answer_test','hash',1)");
        Long admin = jdbc.queryForObject("SELECT id FROM admin WHERE username='agent_answer_test'", Long.class);
        String token = "Bearer " + jwt.issueAdminToken(admin);
        var arguments = json.readTree("""
                {"questions":[{"id":"channel","type":"single_choice","title":"选择渠道？","allow_custom_input":false,
                "options":[{"label":"甲","description":"说明甲"},{"label":"乙","description":"说明乙"},
                {"label":"丙","description":"说明丙"},{"label":"丁","description":"说明丁"}]},
                {"id":"detail","type":"text","title":"补充说明？"}]}
                """);
        when(model.generate(anyList())).thenReturn(new AgentModelResult(null, "ask_user_questions", arguments),
                new AgentModelResult("已收到", null, null));
        String response = mvc.perform(post("/api/admin/agent/chat").header("Authorization",token)
                .contentType("application/json").content("{\"message\":\"请提问\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.questions[0].options.length()").value(4))
                .andReturn().getResponse().getContentAsString();
        long conversationId = json.readTree(response).path("data").path("conversation_id").asLong();
        long questionId = json.readTree(response).path("data").path("question_message_id").asLong();
        String prefix = "{\"conversation_id\":" + conversationId + ",\"question_message_id\":" + questionId + ",\"answers\":";
        mvc.perform(post("/api/admin/agent/chat").header("Authorization",token).contentType("application/json")
                .content(prefix + "{\"channel\":\"无效\",\"detail\":\"内容\"}}"))
                .andExpect(status().isBadRequest());
        String valid = prefix + "{\"channel\":\"甲\",\"detail\":\"内容\"}}";
        mvc.perform(post("/api/admin/agent/chat").header("Authorization",token).contentType("application/json").content(valid))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.answer").value("已收到"));
        mvc.perform(post("/api/admin/agent/chat").header("Authorization",token).contentType("application/json").content(valid))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/admin/agent/conversations/{id}/messages",conversationId).header("Authorization",token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.list[2].content.parts[1].answers.channel").value("甲"));
    }
    @Test void savesConversationAndRestrictsAccess() throws Exception {
        jdbc.update("INSERT INTO admin(username,password_hash,enabled) VALUES ('agent_test_a','hash',1),('agent_test_b','hash',1)");
        Long a=jdbc.queryForObject("SELECT id FROM admin WHERE username='agent_test_a'",Long.class);
        Long b=jdbc.queryForObject("SELECT id FROM admin WHERE username='agent_test_b'",Long.class);
        String token="Bearer "+jwt.issueAdminToken(a); String other="Bearer "+jwt.issueAdminToken(b);
        when(model.generate(anyList())).thenReturn(new AgentModelResult("你好",null,null));
        mvc.perform(post("/api/admin/agent/chat").contentType("application/json").content("{\"message\":\"你好\"}")).andExpect(status().isUnauthorized());
        String result=mvc.perform(post("/api/admin/agent/chat").header("Authorization",token).contentType("application/json").content("{\"message\":\"你好\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("completed")).andReturn().getResponse().getContentAsString();
        long id=json.readTree(result).path("data").path("conversation_id").asLong();
        mvc.perform(get("/api/admin/agent/conversations/{id}/messages",id).header("Authorization",token)).andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(2));
        mvc.perform(get("/api/admin/agent/conversations/{id}/messages",id).header("Authorization",other)).andExpect(status().isNotFound());
        mvc.perform(get("/api/admin/agent/conversations").header("Authorization",token)).andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1));
        mvc.perform(post("/api/admin/agent/chat").header("Authorization",token).contentType("application/json").content("{\"conversation_id\":"+id+",\"message\":\"继续\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/admin/agent/conversations/{id}/messages",id).header("Authorization",token)).andExpect(jsonPath("$.data.total").value(4));
        mvc.perform(get("/api/admin/agent/conversations").header("Authorization","Bearer "+jwt.issueMemberToken(1L))).andExpect(status().isForbidden());
    }

    @Test void queriesTodayBusinessDataThroughRegisteredTool() throws Exception {
        jdbc.update("INSERT INTO admin(username,password_hash,enabled) VALUES ('agent_business_test','hash',1)");
        Long admin = jdbc.queryForObject("SELECT id FROM admin WHERE username='agent_business_test'", Long.class);
        when(model.generate(anyList())).thenReturn(
                new AgentModelResult(null, "get_today_business_data", json.readTree("{}"), "business-call"),
                new AgentModelResult("今日经营数据已查询", null, null));
        mvc.perform(post("/api/admin/agent/chat")
                        .header("Authorization", "Bearer " + jwt.issueAdminToken(admin))
                        .contentType("application/json").content("{\"message\":\"今日营业情况\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("completed"))
                .andExpect(jsonPath("$.data.answer").value("今日经营数据已查询"));
        var captor = org.mockito.ArgumentCaptor.forClass(java.util.List.class);
        org.mockito.Mockito.verify(model, org.mockito.Mockito.times(2)).generate(captor.capture());
        var messages = captor.getAllValues().get(1);
        var result = (com.qianxun.homestay.modules.agent.core.schema.AgentModelMessage) messages.get(messages.size() - 1);
        var data = json.readTree(result.content());
        org.junit.jupiter.api.Assertions.assertEquals("business-call", result.toolCallId());
        org.junit.jupiter.api.Assertions.assertTrue(data.has("paid_amount"));
        org.junit.jupiter.api.Assertions.assertFalse(data.has("guest_name"));
    }

    @Test void queriesRoomStatusThroughRegisteredTool() throws Exception {
        jdbc.update("INSERT INTO admin(username,password_hash,enabled) VALUES ('agent_room_test','hash',1)");
        Long admin = jdbc.queryForObject("SELECT id FROM admin WHERE username='agent_room_test'", Long.class);
        jdbc.update("INSERT INTO room (number, name, floor, price, capacity, enabled) VALUES ('909', '测试房', 1, 300, 2, 1)");
        Long room = jdbc.queryForObject("SELECT id FROM room WHERE number='909'", Long.class);
        jdbc.update("INSERT INTO orders (order_no, room_id, channel_id, guest_name, check_in, check_out, status) VALUES ('QX202611300909', ?, 2, '测试预订人', '2026-11-30', '2026-12-02', 'PENDING_CHECKIN')", room);
        when(model.generate(anyList())).thenReturn(
                new AgentModelResult(null, "get_room_status_by_date", json.readTree("{\"date\":\"2026-12-01\",\"room_number\":\"909\"}"), "room-call"),
                new AgentModelResult("909 房待入住", null, null));
        mvc.perform(post("/api/admin/agent/chat").contentType("application/json").content("{\"message\":\"查房态\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/admin/agent/chat").header("Authorization", "Bearer " + jwt.issueAdminToken(admin))
                        .contentType("application/json").content("{\"message\":\"查 909 房态\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.answer").value("909 房待入住"));
        var captor = org.mockito.ArgumentCaptor.forClass(java.util.List.class);
        org.mockito.Mockito.verify(model, org.mockito.Mockito.times(2)).generate(captor.capture());
        var messages = captor.getAllValues().get(1);
        var result = (com.qianxun.homestay.modules.agent.core.schema.AgentModelMessage) messages.get(messages.size() - 1);
        var roomResult = json.readTree(result.content()).path("rooms").get(0);
        org.junit.jupiter.api.Assertions.assertEquals("测试预订人", roomResult.path("guest_name").asText());
        org.junit.jupiter.api.Assertions.assertEquals("PENDING_CHECKIN", roomResult.path("order_status").asText());
        org.junit.jupiter.api.Assertions.assertEquals("2026-11-30", roomResult.path("check_in").asText());
        org.junit.jupiter.api.Assertions.assertFalse(roomResult.has("phone"));
    }
}
