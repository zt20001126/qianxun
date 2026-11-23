package com.qianxun.homestay.modules.invitation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qianxun.homestay.modules.auth.WechatLoginTestSupport;
import com.qianxun.homestay.modules.auth.service.WechatIdentityService;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
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

/** 使用 MySQL 验证邀请绑定、脱敏与并发约束。 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
    "app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
    "app.jwt.member-expiration-seconds=2592000",
    "app.jwt.admin-expiration-seconds=2592000",
    "app.auth.default-avatar=/assets/avatar-default.png"
})
@AutoConfigureMockMvc
class InvitationApiIntegrationTest {
    @MockitoBean private WechatIdentityService wechatIdentityService;
    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("qianxun_invitation_test").withUsername("qianxun").withPassword("test-password");
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private JdbcTemplate db;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @BeforeEach
    void clean() {
        db.update("DELETE FROM member_invitation");
        db.update("DELETE FROM members");
    }

    @Test
    void bindingIsPermanentPrivateAndAllowsMutualInvitations() throws Exception {
        String alice = login("13800000001");
        String bob = login("13800000002");
        String carol = login("13800000003");
        String aliceCode = code(alice);
        String bobCode = code(bob);
        assertThat(code(alice)).isEqualTo(aliceCode);
        bind(alice, aliceCode);
        assertThat(db.queryForObject("SELECT COUNT(*) FROM member_invitation", Integer.class)).isZero();
        bind(bob, aliceCode);
        bind(bob, code(carol));
        bind(bob, aliceCode);
        bind(alice, bobCode);
        mvc.perform(get("/api/app/invitations").header("Authorization", "Bearer " + alice))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].masked_phone").value("138****0002"))
                .andExpect(jsonPath("$.data.list[0].bound_at").exists());
        mvc.perform(get("/api/app/invitations").header("Authorization", "Bearer " + carol))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(0));
        mvc.perform(get("/api/app/invitations")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/app/invitations/bind").header("Authorization", "Bearer " + bob)
                .contentType("application/json").content("{\"invite_code\":\"bad\"}"))
                .andExpect(status().isBadRequest());
        assertThat(db.queryForObject("SELECT COUNT(*) FROM member_invitation", Integer.class)).isEqualTo(2);
    }

    @Test
    void concurrentBindingAndCodeGenerationRemainUnique() throws Exception {
        String alice = login("13800000011");
        String bob = login("13800000012");
        String carol = login("13800000013");
        try (var executor = Executors.newFixedThreadPool(4)) {
            var codes = executor.invokeAll(List.<Callable<String>>of(() -> code(alice), () -> code(alice)));
            String aliceCode = codes.get(0).get();
            assertThat(codes.get(1).get()).isEqualTo(aliceCode);
            String carolCode = code(carol);
            var bindings = executor.invokeAll(List.<Callable<Void>>of(
                    () -> { bind(bob, aliceCode); return null; },
                    () -> { bind(bob, carolCode); return null; }));
            for (var result : bindings) result.get();
            assertThat(db.queryForObject("SELECT COUNT(*) FROM member_invitation", Integer.class)).isEqualTo(1);
        }
    }

    private String login(String phone) throws Exception {
        WechatLoginTestSupport.stub(wechatIdentityService, phone);
        String body = mvc.perform(post("/api/app/auth/login").contentType("application/json")
                .content(WechatLoginTestSupport.request(phone)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).path("data").path("token").asText();
    }

    private String code(String token) throws Exception {
        String body = mvc.perform(post("/api/app/invitations/share-code").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).path("data").path("invite_code").asText();
    }

    private void bind(String token, String code) throws Exception {
        mvc.perform(post("/api/app/invitations/bind").header("Authorization", "Bearer " + token)
                .contentType("application/json").content("{\"invite_code\":\"" + code + "\"}"))
                .andExpect(status().isOk());
    }
}
