package com.qianxun.homestay.modules.refund;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qianxun.homestay.common.security.service.JwtService;
import com.qianxun.homestay.modules.refund.service.WechatRefundTransactionService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.AfterEach;
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

/** 使用真实 MySQL 8 验证 O03/P03 自助模拟退款事务与幂等行为。 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "app.jwt.member-expiration-seconds=2592000",
        "app.jwt.admin-expiration-seconds=28800",
        "app.auth.default-avatar=/assets/default.png"
})
@AutoConfigureMockMvc
class RefundApiIntegrationTest {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final String PHONE = "13800000041";
    private static final String ORDER_NO = "QX202611180041";
    private static final String REFUND_CONSTRAINT = "chk_fail_return_points_for_test";

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("qianxun_refund_test")
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
    @Autowired
    private WechatRefundTransactionService wechatRefundTransactionService;

    private long memberId;
    private long orderId;
    private String memberToken;

    @DynamicPropertySource
    static void registerDatabaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @BeforeEach
    void preparePaidOrder() throws Exception {
        removeFailureConstraint();
        jdbcTemplate.update("DELETE FROM refunds");
        jdbcTemplate.update("DELETE FROM points_log");
        jdbcTemplate.update("DELETE FROM order_amount");
        jdbcTemplate.update("DELETE FROM payments");
        jdbcTemplate.update("DELETE FROM orders");
        jdbcTemplate.update("DELETE FROM booking_lock");
        jdbcTemplate.update("DELETE FROM room_block");
        jdbcTemplate.update("DELETE FROM room");
        jdbcTemplate.update("DELETE FROM members");

        jdbcTemplate.update(
                "INSERT INTO members (phone, nickname, avatar, growth_points, points) VALUES (?, '0041', '/assets/default.png', 0, 40)",
                PHONE);
        memberId = jdbcTemplate.queryForObject("SELECT id FROM members WHERE phone = ?", Long.class, PHONE);
        memberToken = loginMemberToken(PHONE);

        jdbcTemplate.update(
                "INSERT INTO room (number, name, floor, price, capacity, enabled) VALUES ('991', '退款测试房', 9, 100.00, 4, 1)");
        long roomId = jdbcTemplate.queryForObject("SELECT id FROM room WHERE number = '991'", Long.class);
        LocalDate checkIn = LocalDate.now(BUSINESS_ZONE).plusDays(5);
        LocalDate checkOut = checkIn.plusDays(2);
        jdbcTemplate.update(
                "INSERT INTO booking_lock (member_id, room_id, check_in, check_out, guest_name, phone, guest_count, room_amount, discount_amount, points_used, amount, status, expires_at) VALUES (?, ?, ?, ?, '顾客', ?, 2, 100.00, 2.00, 60, 98.00, 'PAID', ?)",
                memberId, roomId, checkIn, checkOut, PHONE, checkIn.atStartOfDay());
        long bookingId = jdbcTemplate.queryForObject("SELECT id FROM booking_lock WHERE member_id = ?", Long.class, memberId);
        jdbcTemplate.update(
                "INSERT INTO orders (order_no, room_id, channel_id, member_id, guest_name, phone, guest_count, check_in, check_out, status) VALUES (?, ?, 1, ?, '顾客', ?, 2, ?, ?, 'PENDING_CONFIRM')",
                ORDER_NO, roomId, memberId, PHONE, checkIn, checkOut);
        orderId = jdbcTemplate.queryForObject("SELECT id FROM orders WHERE order_no = ?", Long.class, ORDER_NO);
        jdbcTemplate.update(
                "INSERT INTO payments (booking_id, order_id, payment_no, amount, status, paid_at) VALUES (?, ?, 'PAY-REFUND-TEST', 98.00, 'SUCCESS', CURRENT_TIMESTAMP(3))",
                bookingId, orderId);
        jdbcTemplate.update(
                "INSERT INTO points_log (member_id, order_id, type, points, growth_points, balance_after, discount_amount, ref_no, remark) VALUES (?, ?, 'REDEEM', -60, 0, 40, 2.00, ?, '订单积分抵扣')",
                memberId, orderId, "REDEEM:ORDER:" + orderId);
    }

    @AfterEach
    void cleanupFailureConstraint() {
        removeFailureConstraint();
    }

    @Test
    void eligibilityAndRefundAreOwnedAtomicAndIdempotent() throws Exception {
        mockMvc.perform(get("/api/app/orders/{id}/cancel-eligibility", orderId)
                        .header("Authorization", bearer(memberToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.eligible").value(true))
                .andExpect(jsonPath("$.data.reason").value(""))
                .andExpect(jsonPath("$.data.deadline").value(org.hamcrest.Matchers.endsWith("+08:00")));

        mockMvc.perform(get("/api/app/orders/{id}/refunds/latest", orderId)
                        .header("Authorization", bearer(memberToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));

        String firstResponse = mockMvc.perform(post("/api/app/orders/{id}/refunds", orderId)
                        .header("Authorization", bearer(memberToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andReturn().getResponse().getContentAsString();
        JsonNode firstResult = objectMapper.readTree(firstResponse).path("data");

        mockMvc.perform(post("/api/app/orders/{id}/refunds", orderId)
                        .header("Authorization", bearer(memberToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refund_id").value(firstResult.path("refund_id").asLong()))
                .andExpect(jsonPath("$.data.refund_no").value(firstResult.path("refund_no").asText()));

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM orders WHERE id = ?", String.class, orderId))
                .isEqualTo("CANCELLED");
        assertThat(jdbcTemplate.queryForObject("SELECT amount FROM refunds WHERE order_id = ?", BigDecimal.class, orderId))
                .isEqualByComparingTo("98.00");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM refunds WHERE order_id = ?", Integer.class, orderId))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT points FROM members WHERE id = ?", Integer.class, memberId))
                .isEqualTo(100);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM points_log WHERE ref_no = ?", Integer.class, "RETURN:ORDER:" + orderId))
                .isEqualTo(1);

        mockMvc.perform(get("/api/app/orders/{id}/refunds/latest", orderId)
                        .header("Authorization", bearer(memberToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));
    }

    @Test
    void wechatRefundWaitsForConfirmedResultThenCancelsAndReturnsPointsOnce() {
        jdbcTemplate.update("UPDATE payments SET payment_method = 'WECHAT', transaction_id = 'wx-order-tx' WHERE order_id = ?",
                orderId);
        var command = wechatRefundTransactionService.prepare(memberId, orderId, "行程有变");

        assertThat(command.status().name()).isEqualTo("PENDING");
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM orders WHERE id = ?", String.class, orderId))
                .isEqualTo("PENDING_CONFIRM");
        assertThat(jdbcTemplate.queryForObject("SELECT points FROM members WHERE id = ?", Integer.class, memberId))
                .isEqualTo(40);

        var completed = wechatRefundTransactionService.complete(
                command.refundNo(), "PAY-REFUND-TEST", "wx-order-tx", new BigDecimal("98.00"),
                new BigDecimal("98.00"), "wx-refund-id");
        var duplicate = wechatRefundTransactionService.complete(
                command.refundNo(), "PAY-REFUND-TEST", "wx-order-tx", new BigDecimal("98.00"),
                new BigDecimal("98.00"), "wx-refund-id");

        assertThat(completed.status().name()).isEqualTo("SUCCESS");
        assertThat(duplicate.refundId()).isEqualTo(completed.refundId());
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM orders WHERE id = ?", String.class, orderId))
                .isEqualTo("CANCELLED");
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM refunds WHERE refund_no = ?", String.class,
                command.refundNo())).isEqualTo("SUCCESS");
        assertThat(jdbcTemplate.queryForObject("SELECT points FROM members WHERE id = ?", Integer.class, memberId))
                .isEqualTo(100);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM points_log WHERE ref_no = ?", Integer.class, "RETURN:ORDER:" + orderId))
                .isEqualTo(1);
    }

    @Test
    void failedWechatRefundDoesNotCancelOrderOrReturnPoints() {
        jdbcTemplate.update("UPDATE payments SET payment_method = 'WECHAT', transaction_id = 'wx-order-tx' WHERE order_id = ?",
                orderId);
        var command = wechatRefundTransactionService.prepare(memberId, orderId, "行程有变");
        wechatRefundTransactionService.fail(command.refundNo(), "微信退款状态：CLOSED");

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM refunds WHERE refund_no = ?", String.class,
                command.refundNo())).isEqualTo("FAILED");
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM orders WHERE id = ?", String.class, orderId))
                .isEqualTo("PENDING_CONFIRM");
        assertThat(jdbcTemplate.queryForObject("SELECT points FROM members WHERE id = ?", Integer.class, memberId))
                .isEqualTo(40);
    }

    @Test
    void checkedInOrderIsRejectedAndDoesNotChangeRefundOrPoints() throws Exception {
        jdbcTemplate.update("UPDATE orders SET status = 'CHECKED_IN' WHERE id = ?", orderId);

        mockMvc.perform(get("/api/app/orders/{id}/cancel-eligibility", orderId)
                        .header("Authorization", bearer(memberToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.eligible").value(false));
        mockMvc.perform(post("/api/app/orders/{id}/refunds", orderId)
                        .header("Authorization", bearer(memberToken)))
                .andExpect(status().isConflict());

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM refunds", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT points FROM members WHERE id = ?", Integer.class, memberId))
                .isEqualTo(40);
    }

    @Test
    void cancellationReasonIsLimitedToThreeHundredCharacters() throws Exception {
        String requestBody = "{\"reason\":\"" + "x".repeat(301) + "\"}";

        mockMvc.perform(post("/api/app/orders/{id}/refunds", orderId)
                        .header("Authorization", bearer(memberToken))
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM refunds", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM orders WHERE id = ?", String.class, orderId))
                .isEqualTo("PENDING_CONFIRM");
    }

    @Test
    void failedPointsWriteRollsBackRefundAndOrderCancellation() throws Exception {
        jdbcTemplate.execute("ALTER TABLE points_log ADD CONSTRAINT " + REFUND_CONSTRAINT
                + " CHECK (type <> 'RETURN')");

        mockMvc.perform(post("/api/app/orders/{id}/refunds", orderId)
                        .header("Authorization", bearer(memberToken)))
                .andExpect(status().isInternalServerError());

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM orders WHERE id = ?", String.class, orderId))
                .isEqualTo("PENDING_CONFIRM");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM refunds", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT points FROM members WHERE id = ?", Integer.class, memberId))
                .isEqualTo(40);
    }

    @Test
    void anotherMemberCannotReadEligibilityOrRefundOrder() throws Exception {
        String otherMemberToken = loginMemberToken("13800000042");

        mockMvc.perform(get("/api/app/orders/{id}/cancel-eligibility", orderId)
                        .header("Authorization", bearer(otherMemberToken)))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/app/orders/{id}/refunds", orderId)
                        .header("Authorization", bearer(otherMemberToken)))
                .andExpect(status().isNotFound());

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM refunds", Integer.class)).isZero();
    }

    @Test
    void adminCanFilterPaymentAndRefundLedgers() throws Exception {
        mockMvc.perform(post("/api/app/orders/{id}/refunds", orderId)
                        .header("Authorization", bearer(memberToken))
                        .contentType("application/json")
                        .content("{\"reason\":\"行程有变\"}"))
                .andExpect(status().isOk());

        String adminToken = jwtService.issueAdminToken(1L);
        String today = LocalDate.now(BUSINESS_ZONE).toString();
        mockMvc.perform(get("/api/admin/payments")
                        .header("Authorization", bearer(adminToken))
                        .param("status", "SUCCESS")
                        .param("start_date", today)
                        .param("end_date", today))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].payment_no").value("PAY-REFUND-TEST"))
                .andExpect(jsonPath("$.data.list[0].amount").value("98.00"));
        mockMvc.perform(get("/api/admin/refunds")
                        .header("Authorization", bearer(adminToken))
                        .param("status", "SUCCESS")
                        .param("start_date", today)
                        .param("end_date", today))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].order_id").value(orderId))
                .andExpect(jsonPath("$.data.list[0].reason").value("行程有变"));
    }

    private String loginMemberToken(String phone) throws Exception {
        String response = mockMvc.perform(post("/api/app/auth/login")
                        .contentType("application/json")
                        .content("{\"phone\":\"" + phone + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("token").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private void removeFailureConstraint() {
        Integer constraintCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.table_constraints "
                        + "WHERE constraint_schema = DATABASE() AND table_name = 'points_log' AND constraint_name = ?",
                Integer.class,
                REFUND_CONSTRAINT);
        if (constraintCount != null && constraintCount > 0) {
            jdbcTemplate.execute("ALTER TABLE points_log DROP CHECK " + REFUND_CONSTRAINT);
        }
    }
}
