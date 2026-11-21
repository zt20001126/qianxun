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

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.qianxun.homestay.modules.auth.service.WechatIdentityService testWechatIdentity;

    @org.junit.jupiter.api.BeforeEach
    void configureTestWechatIdentity() {
        org.mockito.Mockito.lenient().when(testWechatIdentity.resolveOpenId(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> "openid-" + invocation.<String>getArgument(0));
        org.mockito.Mockito.lenient().when(testWechatIdentity.resolvePhone(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> invocation.<String>getArgument(0));
    }


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
    @Autowired
    private com.qianxun.homestay.common.security.service.JwtService jwtService;

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
                        .content("{\"login_code\":\"" + MEMBER_PHONE + "\",\"phone_code\":\"" + MEMBER_PHONE + "\"}"))
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
    void repeatedWechatLoginReusesExistingMemberAndPreservesProfile() throws Exception {
        jdbcTemplate.update("INSERT INTO members (phone, nickname, avatar) VALUES (?, ?, ?)",
                MEMBER_PHONE, "旧昵称", "https://example.invalid/old.png");
        long originalId = jdbcTemplate.queryForObject("SELECT id FROM members WHERE phone = ?", Long.class,
                MEMBER_PHONE);

        mockMvc.perform(post("/api/app/auth/login")
                        .contentType("application/json")
                        .content("{\"login_code\":\"" + MEMBER_PHONE + "\",\"phone_code\":\"" + MEMBER_PHONE + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.member.id").value(originalId))
                .andExpect(jsonPath("$.data.member.nickname").value("旧昵称"))
                .andExpect(jsonPath("$.data.member.avatar").value("https://example.invalid/old.png"));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM members WHERE phone = ?", Integer.class,
                MEMBER_PHONE)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT openid FROM members WHERE phone = ?", String.class,
                MEMBER_PHONE)).isEqualTo("openid-" + MEMBER_PHONE);
    }

    @Test
    void adminCreatedMemberIsBoundByVerifiedWechatPhone() throws Exception {
        String response = mockMvc.perform(post("/api/admin/members")
                        .header("Authorization", "Bearer " + jwtService.issueAdminToken(1L))
                        .contentType("application/json")
                        .content("{\"phone\":\"" + MEMBER_PHONE + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(response).path("data").path("id").asLong();
        jdbcTemplate.update("UPDATE members SET points = 70, growth_points = 100 WHERE id = ?", id);
        mockMvc.perform(post("/api/app/auth/login").contentType("application/json")
                        .content("{\"login_code\":\"" + MEMBER_PHONE + "\",\"phone_code\":\"" + MEMBER_PHONE + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.member.id").value(id));
        assertThat(jdbcTemplate.queryForObject("SELECT points FROM members WHERE id = ?", Integer.class, id)).isEqualTo(70);
        assertThat(jdbcTemplate.queryForObject("SELECT growth_points FROM members WHERE id = ?", Integer.class, id)).isEqualTo(100);
    }

    @Test
    void existingDifferentBindingCannotBeReplaced() throws Exception {
        jdbcTemplate.update("INSERT INTO members (phone, openid, nickname, avatar, points, growth_points) VALUES (?, ?, ?, ?, 42, 80)",
                MEMBER_PHONE, "other-openid", "member", "/avatar");
        mockMvc.perform(post("/api/app/auth/login").contentType("application/json")
                        .content("{\"login_code\":\"" + MEMBER_PHONE + "\",\"phone_code\":\"" + MEMBER_PHONE + "\"}"))
                .andExpect(status().isConflict());
        assertThat(jdbcTemplate.queryForObject("SELECT openid FROM members WHERE phone = ?", String.class,
                MEMBER_PHONE)).isEqualTo("other-openid");
        assertThat(jdbcTemplate.queryForObject("SELECT points FROM members WHERE phone = ?", Integer.class,
                MEMBER_PHONE)).isEqualTo(42);
    }

    @Test
    void openIdAlreadyBoundToAnotherPhoneCannotCreateOrRebindMember() throws Exception {
        jdbcTemplate.update("INSERT INTO members (phone, openid, nickname, avatar) VALUES (?, ?, ?, ?)",
                "13800000001", "openid-" + MEMBER_PHONE, "other", "/avatar");
        mockMvc.perform(post("/api/app/auth/login").contentType("application/json")
                        .content("{\"login_code\":\"" + MEMBER_PHONE + "\",\"phone_code\":\"" + MEMBER_PHONE + "\"}"))
                .andExpect(status().isConflict());
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM members WHERE phone = ?", Integer.class,
                MEMBER_PHONE)).isZero();
    }

    @Test
    void legacyMemberTokenIsRejectedWhileLegacyAdminTokenStillWorks() throws Exception {
        var key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                io.jsonwebtoken.io.Decoders.BASE64.decode("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="));
        String oldMemberToken = io.jsonwebtoken.Jwts.builder().subject("1").claim("role", "MEMBER")
                .claim("member_id", 1L).signWith(key).compact();
        mockMvc.perform(get("/api/app/auth/me").header("Authorization", "Bearer " + oldMemberToken))
                .andExpect(status().isUnauthorized());
        jdbcTemplate.update("INSERT INTO admin (id, username, password_hash, enabled) VALUES (?, ?, ?, 1)",
                987L, ADMIN_USERNAME, passwordEncoder.encode(ADMIN_PASSWORD));
        String oldAdminToken = io.jsonwebtoken.Jwts.builder().subject("987").claim("role", "ADMIN")
                .claim("admin_id", 987L).signWith(key).compact();
        mockMvc.perform(get("/api/admin/auth/me").header("Authorization", "Bearer " + oldAdminToken))
                .andExpect(status().isOk());
    }

    @Test
    void simultaneousFirstLoginCreatesOnlyOneMember() throws Exception {
        var executor = Executors.newFixedThreadPool(8);
        try {
            List<Callable<Long>> loginCalls = java.util.stream.IntStream.range(0, 8)
                    .<Callable<Long>>mapToObj(index -> () -> {
                        String response = mockMvc.perform(post("/api/app/auth/login")
                                        .contentType("application/json")
                                        .content("{\"login_code\":\"" + MEMBER_PHONE + "\",\"phone_code\":\"" + MEMBER_PHONE + "\"}"))
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
    void concurrentDifferentWechatAccountsCannotBindSamePhone() throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        var start = new java.util.concurrent.CountDownLatch(1);
        try {
            List<Callable<Integer>> calls = java.util.stream.IntStream.range(0, 2)
                    .<Callable<Integer>>mapToObj(index -> () -> {
                        start.await();
                        return mockMvc.perform(post("/api/app/auth/login").contentType("application/json")
                                        .content("{\"login_code\":\"account-" + index
                                                + "\",\"phone_code\":\"" + MEMBER_PHONE + "\"}"))
                                .andReturn().getResponse().getStatus();
                    }).toList();
            var first = executor.submit(calls.get(0));
            var second = executor.submit(calls.get(1));
            start.countDown();
            assertThat(List.of(first.get(), second.get())).containsExactlyInAnyOrder(200, 409);
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
