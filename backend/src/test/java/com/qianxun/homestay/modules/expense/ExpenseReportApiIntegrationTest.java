package com.qianxun.homestay.modules.expense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
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

/** 使用 MySQL 8 验证经营支出迁移、E01–E06 接口和 S01–S05 报表口径。 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "app.jwt.member-expiration-seconds=2592000",
        "app.jwt.admin-expiration-seconds=28800",
        "app.auth.default-avatar=/assets/avatar-default.png"
})
@AutoConfigureMockMvc
class ExpenseReportApiIntegrationTest {

    private static final String ADMIN_USERNAME = "stage-nine-admin";
    private static final String ADMIN_PASSWORD = "stage-nine-password";
    private static final LocalDate CHECK_IN = LocalDate.of(2026, 11, 15);
    private static final LocalDate CHECK_OUT = LocalDate.of(2026, 11, 17);

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("qianxun_stage_nine_test")
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

    private String adminToken;
    private long roomId;

    @DynamicPropertySource
    static void registerDatabaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @BeforeEach
    void resetStageNineData() throws Exception {
        jdbcTemplate.update("DELETE FROM expenses");
        jdbcTemplate.update("DELETE FROM order_amount");
        jdbcTemplate.update("DELETE FROM orders");
        jdbcTemplate.update("DELETE FROM room_block");
        jdbcTemplate.update("DELETE FROM creators");
        jdbcTemplate.update("DELETE FROM room");
        jdbcTemplate.update("DELETE FROM members");
        jdbcTemplate.update("DELETE FROM admin");
        jdbcTemplate.update(
                "INSERT INTO admin (username, password_hash, enabled) VALUES (?, ?, 1)",
                ADMIN_USERNAME, passwordEncoder.encode(ADMIN_PASSWORD));
        jdbcTemplate.update(
                "INSERT INTO room (number, name, floor, price, capacity, enabled) VALUES (?, ?, 1, ?, 4, 1)",
                "101", "枕澜大床房", "300.00");
        roomId = jdbcTemplate.queryForObject("SELECT id FROM room WHERE number = '101'", Long.class);
        insertCheckedInOrder();
        jdbcTemplate.update("INSERT INTO expenses (content, amount, date) VALUES (?, ?, ?)",
                "本月维护", new java.math.BigDecimal("10.00"), LocalDate.of(2026, 11, 16));
        adminToken = loginAdminToken();
    }

    @Test
    void expenseEndpointsEnforceAdminAccessAndKeepRecordsImmutable() throws Exception {
        mockMvc.perform(get("/api/admin/expenses"))
                .andExpect(status().isUnauthorized());

        String memberToken = loginMemberToken();
        mockMvc.perform(get("/api/admin/expenses").header("Authorization", bearer(memberToken)))
                .andExpect(status().isForbidden());

        String invalidRequest = """
                {"content":"维修","amount":"12.345","date":"2026-11-18"}
                """;
        mockMvc.perform(post("/api/admin/expenses")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        String createRequest = """
                {"content":"补充洗漱用品","amount":"12.30","date":"2026-11-18"}
                """;
        String createdBody = mockMvc.perform(post("/api/admin/expenses")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(createRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amount").value("12.30"))
                .andExpect(jsonPath("$.data.date").value("2026-11-18"))
                .andExpect(jsonPath("$.data.created_at").value(org.hamcrest.Matchers.endsWith("+08:00")))
                .andReturn().getResponse().getContentAsString();
        long expenseId = objectMapper.readTree(createdBody).path("data").path("id").asLong();

        mockMvc.perform(get("/api/admin/expenses")
                        .header("Authorization", bearer(adminToken))
                        .param("start_date", "2026-11-01")
                        .param("end_date", "2026-11-30")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.list[0].id").value(expenseId));

        mockMvc.perform(get("/api/admin/expenses/{id}", expenseId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("补充洗漱用品"));

        mockMvc.perform(get("/api/admin/expenses/summary")
                        .header("Authorization", bearer(adminToken))
                        .param("start_date", "2026-11-01")
                        .param("end_date", "2026-11-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total_amount").value("22.30"))
                .andExpect(jsonPath("$.data.count").value(2))
                .andExpect(jsonPath("$.data.daily.length()").value(2));

        mockMvc.perform(put("/api/admin/expenses/{id}", expenseId)
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(delete("/api/admin/expenses/{id}", expenseId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void reportEndpointsApplyNightAllocationScopesAndProfitRules() throws Exception {
        mockMvc.perform(get("/api/admin/reports/overview")
                        .header("Authorization", bearer(adminToken))
                        .param("month", "2026-11")
                        .param("scope", "CHECKED_IN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.order_count").value(1))
                .andExpect(jsonPath("$.data.night_count").value(2))
                .andExpect(jsonPath("$.data.paid_amount").value("101.00"))
                .andExpect(jsonPath("$.data.net_amount").value("100.00"))
                .andExpect(jsonPath("$.data.expense_amount").value("10.00"))
                .andExpect(jsonPath("$.data.operating_profit").value("90.00"))
                .andExpect(jsonPath("$.data.occupancy_rate").value(0.0667));

        mockMvc.perform(get("/api/admin/reports/overview")
                        .header("Authorization", bearer(adminToken))
                        .param("month", "2026-11")
                        .param("scope", "NOT_CHECKED_IN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.order_count").value(0))
                .andExpect(jsonPath("$.data.paid_amount").value("0.00"))
                .andExpect(jsonPath("$.data.operating_profit").value("90.00"));

        String creatorsBody = mockMvc.perform(get("/api/admin/reports/creators")
                        .header("Authorization", bearer(adminToken))
                        .param("month", "2026-11"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode noCreator = objectMapper.readTree(creatorsBody).path("data").path("list").get(0);
        assertThat(noCreator.has("id")).isTrue();
        assertThat(noCreator.get("id").isNull()).isTrue();
        assertThat(noCreator.path("name").asText()).isEqualTo("无主播订单");

        mockMvc.perform(get("/api/admin/reports/rooms")
                        .header("Authorization", bearer(adminToken))
                        .param("month", "2026-11")
                        .param("room_id", String.valueOf(roomId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list[0].room_profit").value("100.00"))
                .andExpect(jsonPath("$.data.list[0].occupancy_rate").value(0.0667));

        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", bearer(adminToken))
                        .param("date", "2026-11-15")
                        .param("month", "2026-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.occupied_rooms").value(1))
                .andExpect(jsonPath("$.data.total_rooms").value(1))
                .andExpect(jsonPath("$.data.rooms[0].status").value("CHECKED_IN"))
                .andExpect(jsonPath("$.data.month_summary.operating_profit").value("90.00"));
    }

    private void insertCheckedInOrder() {
        jdbcTemplate.update("""
                INSERT INTO orders (
                    order_no, room_id, channel_id, guest_name, phone, guest_count,
                    check_in, check_out, status
                ) VALUES (?, ?, 2, ?, ?, 2, ?, ?, 'CHECKED_IN')
                """,
                "QX202611150001", roomId, "测试顾客", "13800000001", CHECK_IN, CHECK_OUT);
        Long orderId = jdbcTemplate.queryForObject(
                "SELECT id FROM orders WHERE order_no = ?", Long.class, "QX202611150001");
        jdbcTemplate.update("""
                INSERT INTO order_amount (
                    order_id, paid_amount, platform_fee, platform_commission, extra_commission, expected_amount
                ) VALUES (?, ?, ?, 0.00, 0.00, NULL)
                """, orderId, new java.math.BigDecimal("101.00"), new java.math.BigDecimal("1.00"));
    }

    private String loginAdminToken() throws Exception {
        String response = mockMvc.perform(post("/api/admin/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"" + ADMIN_USERNAME + "\",\"password\":\"" + ADMIN_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("token").asText();
    }

    private String loginMemberToken() throws Exception {
        String response = mockMvc.perform(post("/api/app/auth/login")
                        .contentType("application/json")
                        .content("{\"phone\":\"13800000009\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("token").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
