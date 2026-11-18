package com.qianxun.homestay.modules.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qianxun.homestay.common.security.service.JwtService;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** 使用真实 MySQL 8 验证会员资料、积分账本、会员订单归属及奖励重试。 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "app.jwt.member-expiration-seconds=2592000",
        "app.jwt.admin-expiration-seconds=28800",
        "app.auth.default-avatar=/assets/avatar-default.png",
        "app.channel.mini-program-id=1"
})
@AutoConfigureMockMvc
class MemberPointsApiIntegrationTest {

    private static final String PHONE = "13800000001";
    private static final String OTHER_PHONE = "13800000002";

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("qianxun_member_test")
            .withUsername("qianxun")
            .withPassword("qianxun-test-password");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private JwtService jwtService;

    private long memberId;
    private long otherMemberId;
    private long roomId;
    private String memberToken;
    private String otherMemberToken;
    private String adminToken;

    @DynamicPropertySource
    static void registerDatabaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @BeforeEach
    void prepareMembersAndRoom() {
        jdbcTemplate.update("DELETE FROM points_log");
        jdbcTemplate.update("DELETE FROM order_amount");
        jdbcTemplate.update("DELETE FROM orders");
        jdbcTemplate.update("DELETE FROM booking_lock");
        jdbcTemplate.update("DELETE FROM room_block");
        jdbcTemplate.update("DELETE FROM creators");
        jdbcTemplate.update("DELETE FROM room");
        jdbcTemplate.update("DELETE FROM members");

        jdbcTemplate.update("INSERT INTO members (phone, nickname, avatar) VALUES (?, ?, '/assets/avatar-default.png')",
                PHONE, "0001");
        jdbcTemplate.update("INSERT INTO members (phone, nickname, avatar) VALUES (?, ?, '/assets/avatar-default.png')",
                OTHER_PHONE, "0002");
        memberId = jdbcTemplate.queryForObject("SELECT id FROM members WHERE phone = ?", Long.class, PHONE);
        otherMemberId = jdbcTemplate.queryForObject("SELECT id FROM members WHERE phone = ?", Long.class, OTHER_PHONE);
        jdbcTemplate.update(
                "INSERT INTO room (number, name, floor, price, capacity, enabled) VALUES ('991', '会员测试房', 9, 100.00, 4, 1)");
        roomId = jdbcTemplate.queryForObject("SELECT id FROM room WHERE number = '991'", Long.class);
        memberToken = jwtService.issueMemberToken(memberId);
        otherMemberToken = jwtService.issueMemberToken(otherMemberId);
        adminToken = jwtService.issueAdminToken(1L);
    }

    @Test
    void profileRulesGrantsAndMemberScopedLogsFollowLedgerRules() throws Exception {
        mockMvc.perform(get("/api/app/members/me").header("Authorization", bearer(memberToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phone").value("138****0001"))
                .andExpect(jsonPath("$.data.level").value("普通"))
                .andExpect(jsonPath("$.data.next_level_points").value(500))
                .andExpect(jsonPath("$.data.progress_percent").value(0));

        mockMvc.perform(get("/api/app/points/rules").header("Authorization", bearer(memberToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.levels.length()").value(4))
                .andExpect(jsonPath("$.data.levels[0].points_per_yuan").value(30))
                .andExpect(jsonPath("$.data.max_discount_rate").value(0.20))
                .andExpect(jsonPath("$.data.expire").value(false));

        String createdResponse = mockMvc.perform(post("/api/admin/members")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content("{\"phone\":\"13800000003\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phone").value("13800000003"))
                .andReturn().getResponse().getContentAsString();
        JsonNode created = objectMapper.readTree(createdResponse).path("data");
        long createdId = created.path("id").asLong();
        assertThat(jdbcTemplate.queryForObject("SELECT points FROM members WHERE id = ?", Integer.class, createdId))
                .isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT openid FROM members WHERE id = ?", String.class, createdId))
                .isNull();
        mockMvc.perform(post("/api/admin/members")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content("{\"phone\":\"13800000003\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/admin/members")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content("{\"phone\":\"12345\"}"))
                .andExpect(status().isBadRequest());

        String historyGrant = "{\"type\":\"HISTORY_REWARD\",\"points\":500,\"remark\":\"历史消费\",\"request_no\":\"stage8-history-1\"}";
        String firstGrant = mockMvc.perform(post("/api/admin/members/{id}/points/grant", createdId)
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(historyGrant))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.growth_points").value(500))
                .andExpect(jsonPath("$.data.balance_after").value(500))
                .andReturn().getResponse().getContentAsString();
        long historyLogId = objectMapper.readTree(firstGrant).path("data").path("log_id").asLong();

        mockMvc.perform(post("/api/admin/members/{id}/points/grant", createdId)
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(historyGrant))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.log_id").value(historyLogId));
        mockMvc.perform(post("/api/admin/members/{id}/points/grant", createdId)
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content("{\"type\":\"HISTORY_REWARD\",\"points\":501,\"remark\":\"历史消费\",\"request_no\":\"stage8-history-1\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/admin/members/{id}/points/grant", createdId)
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content("{\"type\":\"GIFT\",\"points\":100,\"request_no\":\"stage8-gift-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.growth_points").value(0))
                .andExpect(jsonPath("$.data.balance_after").value(600));

        assertThat(jdbcTemplate.queryForObject("SELECT points FROM members WHERE id = ?", Integer.class, createdId))
                .isEqualTo(600);
        assertThat(jdbcTemplate.queryForObject("SELECT growth_points FROM members WHERE id = ?", Integer.class, createdId))
                .isEqualTo(500);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM points_log WHERE member_id = ?", Integer.class, createdId))
                .isEqualTo(2);
        mockMvc.perform(get("/api/admin/members/{id}", createdId).header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.points_logs.length()").value(2))
                .andExpect(jsonPath("$.data.points_log_total").value(2));

        mockMvc.perform(get("/api/app/members/me")
                        .header("Authorization", bearer(memberToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.level").value("普通"));
        mockMvc.perform(get("/api/admin/members")
                        .header("Authorization", bearer(adminToken))
                        .param("phone", "13800000003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].level").value("银"));
        mockMvc.perform(get("/api/app/points/logs")
                        .header("Authorization", bearer(jwtService.issueMemberToken(createdId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.list[0].balance_after").exists());
        mockMvc.perform(get("/api/app/points/logs").header("Authorization", bearer(otherMemberToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
        mockMvc.perform(get("/api/admin/points/logs")
                        .header("Authorization", bearer(adminToken))
                        .param("member_id", String.valueOf(createdId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2));
        mockMvc.perform(post("/api/admin/members/{id}/points/grant", createdId)
                        .header("Authorization", bearer(memberToken))
                        .contentType("application/json")
                        .content("{\"type\":\"GIFT\",\"points\":10,\"request_no\":\"forbidden\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void memberOrdersAreLinkedByMemberIdAndCompletedOrderRewardRetryIsIdempotent() throws Exception {
        long orderId = insertOrder(memberId, 1L, "COMPLETED", "QX202611180901", null, "99.99");
        insertOrder(otherMemberId, 1L, "COMPLETED", "QX202611180902", null, "30.00");

        mockMvc.perform(get("/api/app/members/me/orders").header("Authorization", bearer(memberToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].id").value(orderId));
        mockMvc.perform(get("/api/admin/members/{id}", memberId).header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orders.length()").value(1))
                .andExpect(jsonPath("$.data.points_logs.length()").value(0))
                .andExpect(jsonPath("$.data.points_log_total").value(0))
                .andExpect(jsonPath("$.data.orders[0].paid_amount").value("99.99"));

        mockMvc.perform(post("/api/admin/points/rewards/{orderId}/retry", orderId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rewarded").value(true))
                .andExpect(jsonPath("$.data.log_id").isNumber());
        mockMvc.perform(post("/api/admin/points/rewards/{orderId}/retry", orderId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rewarded").value(true));
        assertThat(jdbcTemplate.queryForObject("SELECT points FROM members WHERE id = ?", Integer.class, memberId))
                .isEqualTo(99);
        assertThat(jdbcTemplate.queryForObject("SELECT growth_points FROM members WHERE id = ?", Integer.class, memberId))
                .isEqualTo(99);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM points_log WHERE ref_no = ?", Integer.class,
                "ORDER_REWARD:" + orderId)).isEqualTo(1);

        long pendingOrderId = insertOrder(memberId, 1L, "PENDING_CONFIRM", "QX202611180903", null, "25.00");
        mockMvc.perform(post("/api/admin/points/rewards/{orderId}/retry", pendingOrderId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isUnprocessableEntity());

        long unlinkedOrderId = insertOrder(null, 2L, "COMPLETED", "QX202611180904", null, "25.00");
        mockMvc.perform(post("/api/admin/points/rewards/{orderId}/retry", unlinkedOrderId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rewarded").value(false))
                .andExpect(jsonPath("$.data.log_id").doesNotExist());
    }

    @Test
    void memberAndAdminEndpointsRejectMissingOrWrongRoleTokens() throws Exception {
        mockMvc.perform(get("/api/app/members/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/members").header("Authorization", bearer(memberToken)))
                .andExpect(status().isForbidden());
    }

    private long insertOrder(Long ownerId, Long channelId, String status, String orderNo, String phone, String paidAmount) {
        LocalDate checkIn = LocalDate.now(ZoneId.of("Asia/Shanghai")).plusDays(10);
        jdbcTemplate.update(
                "INSERT INTO orders (order_no, room_id, channel_id, member_id, guest_name, phone, guest_count, check_in, check_out, status) VALUES (?, ?, ?, ?, '会员测试', ?, 2, ?, ?, ?)",
                orderNo, roomId, channelId, ownerId, phone, checkIn, checkIn.plusDays(1), status);
        long id = jdbcTemplate.queryForObject("SELECT id FROM orders WHERE order_no = ?", Long.class, orderNo);
        jdbcTemplate.update("INSERT INTO order_amount (order_id, paid_amount) VALUES (?, ?)", id, paidAmount);
        return id;
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
