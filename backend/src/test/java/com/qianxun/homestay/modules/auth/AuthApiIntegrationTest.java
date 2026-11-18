package com.qianxun.homestay.modules.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 使用真实 MySQL 8 验证四个认证接口、会员手机号去重和角色隔离。
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "app.jwt.member-expiration-seconds=2592000",
        "app.jwt.admin-expiration-seconds=28800",
        "app.auth.default-avatar=/assets/avatar-default.png"
})
@AutoConfigureMockMvc
class AuthApiIntegrationTest {

    private static final String MEMBER_PHONE = "13800000000";
    private static final String ADMIN_USERNAME = "stage-one-admin";
    private static final String ADMIN_PASSWORD = "test-password-2026";

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("qianxun_auth_test")
            .withUsername("qianxun")
            .withPassword("qianxun-test-password");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @DynamicPropertySource
    static void registerDatabaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @BeforeEach
    void clearAuthenticationRows() {
        jdbcTemplate.update("DELETE FROM admin");
        jdbcTemplate.update("DELETE FROM members");
    }

    @Test
    void appLoginCreatesOneMemberAndAppMeReturnsCurrentMember() throws Exception {
        String loginResponse = mockMvc.perform(post("/api/app/auth/login")
                        .contentType("application/json")
                        .content("{\"phone\":\"" + MEMBER_PHONE + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.expires_in").value(2592000))
                .andExpect(jsonPath("$.data.member.phone").value(MEMBER_PHONE))
                .andExpect(jsonPath("$.data.member.nickname").value("0000"))
                .andExpect(jsonPath("$.data.member.avatar").value("/assets/avatar-default.png"))
                .andReturn().getResponse().getContentAsString();

        JsonNode loginData = objectMapper.readTree(loginResponse).path("data");
        String memberToken = loginData.path("token").asText();
        long memberId = loginData.path("member").path("id").asLong();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM members WHERE phone = ?", Integer.class,
                MEMBER_PHONE)).isEqualTo(1);

        mockMvc.perform(get("/api/app/auth/me").header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(memberId))
                .andExpect(jsonPath("$.data.points").value(0));

        mockMvc.perform(get("/api/app/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        mockMvc.perform(get("/api/admin/auth/me").header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void repeatedPhoneLoginReusesMemberAndResetsFixedProfile() throws Exception {
        jdbcTemplate.update("INSERT INTO members (phone, nickname, avatar) VALUES (?, ?, ?)",
                MEMBER_PHONE, "旧昵称", "https://example.invalid/old.png");
        long originalId = jdbcTemplate.queryForObject("SELECT id FROM members WHERE phone = ?", Long.class,
                MEMBER_PHONE);

        mockMvc.perform(post("/api/app/auth/login")
                        .contentType("application/json")
                        .content("{\"phone\":\"" + MEMBER_PHONE + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.member.id").value(originalId))
                .andExpect(jsonPath("$.data.member.nickname").value("0000"))
                .andExpect(jsonPath("$.data.member.avatar").value("/assets/avatar-default.png"));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM members WHERE phone = ?", Integer.class,
                MEMBER_PHONE)).isEqualTo(1);
    }

    @Test
    void simultaneousFirstLoginCreatesOnlyOneMember() throws Exception {
        var executor = Executors.newFixedThreadPool(8);
        try {
            List<Callable<Long>> loginCalls = java.util.stream.IntStream.range(0, 8)
                    .<Callable<Long>>mapToObj(index -> () -> {
                        String response = mockMvc.perform(post("/api/app/auth/login")
                                        .contentType("application/json")
                                        .content("{\"phone\":\"" + MEMBER_PHONE + "\"}"))
                                .andExpect(status().isOk())
                                .andReturn().getResponse().getContentAsString();
                        return objectMapper.readTree(response).path("data").path("member").path("id").asLong();
                    })
                    .toList();
            var results = executor.invokeAll(loginCalls);
            List<Long> memberIds = results.stream().map(future -> {
                try {
                    return future.get();
                } catch (Exception exception) {
                    throw new IllegalStateException("Concurrent login request failed", exception);
                }
            }).toList();

            assertThat(memberIds).containsOnly(memberIds.getFirst());
            assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM members WHERE phone = ?", Integer.class,
                    MEMBER_PHONE)).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void adminLoginChecksBcryptAndKeepsAdminTokenOutOfMemberArea() throws Exception {
        jdbcTemplate.update("INSERT INTO admin (username, password_hash, enabled) VALUES (?, ?, 1)",
                ADMIN_USERNAME, passwordEncoder.encode(ADMIN_PASSWORD));

        mockMvc.perform(post("/api/admin/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"" + ADMIN_USERNAME + "\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        String loginResponse = mockMvc.perform(post("/api/admin/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"" + ADMIN_USERNAME + "\",\"password\":\""
                                + ADMIN_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.admin.username").value(ADMIN_USERNAME))
                .andExpect(jsonPath("$.data.admin.password_hash").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        String adminToken = objectMapper.readTree(loginResponse).path("data").path("token").asText();
        mockMvc.perform(get("/api/admin/auth/me").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(ADMIN_USERNAME));

        mockMvc.perform(get("/api/app/auth/me").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void loginRejectsInvalidPhoneAndUnauthenticatedAdminMe() throws Exception {
        mockMvc.perform(post("/api/app/auth/login")
                        .contentType("application/json")
                        .content("{\"phone\":\"12345\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(get("/api/admin/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }
}
