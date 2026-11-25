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
}
