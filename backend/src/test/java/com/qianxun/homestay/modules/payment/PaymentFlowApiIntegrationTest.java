package com.qianxun.homestay.modules.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qianxun.homestay.common.security.service.JwtService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** 使用真实 MySQL 8 验证 P01/P08 预订支付闭环、快照、幂等和超时状态。 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "app.jwt.member-expiration-seconds=2592000",
        "app.jwt.admin-expiration-seconds=28800",
        "app.auth.default-avatar=/assets/default.png"
})
@AutoConfigureMockMvc
class PaymentFlowApiIntegrationTest {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final String MEMBER_PHONE = "13800000061";
    private static final String ROOM_NUMBER = "961";
    private static final String GUEST_PHONE = "13800000062";
    private static final int POINTS_TO_USE = 200;
    private static final int THREAD_COUNT = 2;
    private static final String POINTS_FAILURE_CONSTRAINT = "chk_payment_fail_redeem_for_test";

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("qianxun_payment_test")
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
    private long roomId;
    private String memberToken;

    @DynamicPropertySource
    static void registerDatabaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @BeforeEach
    void prepareMemberAndRoom() {
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
                "INSERT INTO members (phone, nickname, avatar, growth_points, points) VALUES (?, '0061', '/assets/default.png', 1000, 200)",
                MEMBER_PHONE);
        memberId = jdbcTemplate.queryForObject("SELECT id FROM members WHERE phone = ?", Long.class, MEMBER_PHONE);
        memberToken = jwtService.issueMemberToken(memberId);

        jdbcTemplate.update(
                "INSERT INTO room (number, name, floor, price, capacity, enabled) VALUES (?, '支付闭环测试房', 9, 100.00, 4, 1)",
                ROOM_NUMBER);
        roomId = jdbcTemplate.queryForObject("SELECT id FROM room WHERE number = ?", Long.class, ROOM_NUMBER);
    }

    @Test
    void prepayAndRepeatedMockPayCreateOneOrderAndConsumePointsOnce() throws Exception {
        JsonNode prepay = prepay();
        String paymentNo = prepay.path("payment_no").asText();

        assertThat(prepay.path("room_amount").asText()).isEqualTo("200.00");
        assertThat(prepay.path("points_used").asInt()).isEqualTo(POINTS_TO_USE);
        assertThat(prepay.path("discount_amount").asText()).isEqualTo("10.00");
        assertThat(prepay.path("pay_amount").asText()).isEqualTo("190.00");

        JsonNode firstResult = mockPay(paymentNo).path("data");
        JsonNode repeatedResult = mockPay(paymentNo).path("data");

        assertThat(firstResult.path("status").asText()).isEqualTo("SUCCESS");
        assertThat(firstResult.path("order_id").asLong()).isPositive();
        assertThat(firstResult.path("order_no").asText()).matches("QX\\d{12}");
        assertThat(firstResult.path("paid_amount").asText()).isEqualTo("190.00");
        assertThat(repeatedResult.path("order_id").asLong()).isEqualTo(firstResult.path("order_id").asLong());
        assertThat(repeatedResult.path("order_no").asText()).isEqualTo(firstResult.path("order_no").asText());

        Long orderId = firstResult.path("order_id").asLong();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM orders WHERE id = ?", String.class, orderId))
                .isEqualTo("PENDING_CONFIRM");
        assertThat(jdbcTemplate.queryForObject("SELECT amount FROM payments WHERE payment_no = ?", String.class,
                paymentNo)).isEqualTo("190.00");
        assertThat(jdbcTemplate.queryForObject("SELECT paid_amount FROM order_amount WHERE order_id = ?", String.class,
                orderId)).isEqualTo("190.00");
        assertThat(jdbcTemplate.queryForObject("SELECT net_amount FROM order_amount WHERE order_id = ?", String.class,
                orderId)).isEqualTo("190.00");
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM booking_lock WHERE id = ?", String.class,
                prepay.path("booking_id").asLong())).isEqualTo("PAID");
        assertThat(jdbcTemplate.queryForObject("SELECT points FROM members WHERE id = ?", Integer.class, memberId))
                .isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM points_log WHERE ref_no = ?", Integer.class, "REDEEM:ORDER:" + orderId))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT balance_after FROM points_log WHERE ref_no = ?", Integer.class, "REDEEM:ORDER:" + orderId))
                .isZero();
    }

    @Test
    void concurrentMockPayRequestsReturnTheSameSingleOrder() throws Exception {
        String paymentNo = prepay().path("payment_no").asText();
        CountDownLatch ready = new CountDownLatch(THREAD_COUNT);
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(THREAD_COUNT);

        try {
            var first = executor.submit(() -> concurrentMockPay(paymentNo, ready, start));
            var second = executor.submit(() -> concurrentMockPay(paymentNo, ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            JsonNode firstResult = objectMapper.readTree(first.get(15, TimeUnit.SECONDS).getResponse().getContentAsString())
                    .path("data");
            JsonNode secondResult = objectMapper.readTree(second.get(15, TimeUnit.SECONDS).getResponse().getContentAsString())
                    .path("data");

            assertThat(firstResult.path("order_id").asLong()).isEqualTo(secondResult.path("order_id").asLong());
            assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders", Integer.class)).isEqualTo(1);
            assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM order_amount", Integer.class)).isEqualTo(1);
            assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM points_log WHERE type = 'REDEEM'", Integer.class))
                    .isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void concurrentPrepayRequestsForTheSameRoomNightOnlyReserveOnce() throws Exception {
        String otherPhone = "13800000063";
        jdbcTemplate.update(
                "INSERT INTO members (phone, nickname, avatar, growth_points, points) VALUES (?, '0063', '/assets/default.png', 1000, 200)",
                otherPhone);
        Long otherMemberId = jdbcTemplate.queryForObject(
                "SELECT id FROM members WHERE phone = ?", Long.class, otherPhone);
        String otherToken = jwtService.issueMemberToken(otherMemberId);
        CountDownLatch ready = new CountDownLatch(THREAD_COUNT);
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(THREAD_COUNT);

        try {
            var first = executor.submit(() -> concurrentPrepay(memberToken, ready, start));
            var second = executor.submit(() -> concurrentPrepay(otherToken, ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            int firstStatus = first.get(15, TimeUnit.SECONDS).getResponse().getStatus();
            int secondStatus = second.get(15, TimeUnit.SECONDS).getResponse().getStatus();
            assertThat(List.of(firstStatus, secondStatus)).containsExactlyInAnyOrder(200, 409);
            assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM booking_lock WHERE status = 'PENDING'",
                    Integer.class)).isEqualTo(1);
            assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM payments WHERE status = 'PENDING'",
                    Integer.class)).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void failedPointsLedgerWriteRollsBackTheMockPaymentAndOrder() throws Exception {
        JsonNode prepay = prepay();
        String paymentNo = prepay.path("payment_no").asText();
        long bookingId = prepay.path("booking_id").asLong();
        jdbcTemplate.execute("ALTER TABLE points_log ADD CONSTRAINT " + POINTS_FAILURE_CONSTRAINT
                + " CHECK (type <> 'REDEEM')");

        try {
            mockMvc.perform(post("/api/app/payments/{paymentNo}/mock-pay", paymentNo)
                            .header("Authorization", bearer(memberToken)))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isInternalServerError());
        } finally {
            jdbcTemplate.execute("ALTER TABLE points_log DROP CHECK " + POINTS_FAILURE_CONSTRAINT);
        }

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM order_amount", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM payments WHERE payment_no = ?", String.class,
                paymentNo)).isEqualTo("PENDING");
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM booking_lock WHERE id = ?", String.class,
                bookingId)).isEqualTo("PENDING");
        assertThat(jdbcTemplate.queryForObject("SELECT points FROM members WHERE id = ?", Integer.class, memberId))
                .isEqualTo(POINTS_TO_USE);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM points_log", Integer.class)).isZero();
    }

    @Test
    void expiredPrepayCannotBePaidAndReleasesItsReservation() throws Exception {
        JsonNode prepay = prepay();
        long bookingId = prepay.path("booking_id").asLong();
        String paymentNo = prepay.path("payment_no").asText();
        jdbcTemplate.update("UPDATE booking_lock SET expires_at = ? WHERE id = ?",
                LocalDate.now(BUSINESS_ZONE).atStartOfDay(), bookingId);

        mockMvc.perform(post("/api/app/payments/{paymentNo}/mock-pay", paymentNo)
                        .header("Authorization", bearer(memberToken)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isConflict());

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM payments WHERE payment_no = ?", String.class,
                paymentNo)).isEqualTo("CLOSED");
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM booking_lock WHERE id = ?", String.class,
                bookingId)).isEqualTo("EXPIRED");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT points FROM members WHERE id = ?", Integer.class, memberId))
                .isEqualTo(POINTS_TO_USE);
    }

    private JsonNode prepay() throws Exception {
        String response = mockMvc.perform(post("/api/app/payments/prepay")
                        .header("Authorization", bearer(memberToken))
                        .contentType("application/json")
                        .content(prepayRequestBody()))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data");
    }

    private String prepayRequestBody() throws Exception {
        LocalDate checkIn = LocalDate.now(BUSINESS_ZONE).plusDays(10);
        LocalDate checkOut = checkIn.plusDays(2);
        return objectMapper.writeValueAsString(Map.of(
                "room_id", roomId,
                "check_in", checkIn,
                "check_out", checkOut,
                "guest_name", "闭环顾客",
                "phone", GUEST_PHONE,
                "guest_count", 2,
                "remark", "阶段10验收",
                "points_used", POINTS_TO_USE));
    }

    private MvcResult concurrentPrepay(String token, CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("并发预支付请求未能同时启动");
        }
        return mockMvc.perform(post("/api/app/payments/prepay")
                        .header("Authorization", bearer(token))
                        .contentType("application/json")
                        .content(prepayRequestBody()))
                .andReturn();
    }

    private JsonNode mockPay(String paymentNo) throws Exception {
        String response = mockMvc.perform(post("/api/app/payments/{paymentNo}/mock-pay", paymentNo)
                        .header("Authorization", bearer(memberToken)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private MvcResult concurrentMockPay(String paymentNo, CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("并发支付请求未能同时启动");
        }
        return mockMvc.perform(post("/api/app/payments/{paymentNo}/mock-pay", paymentNo)
                        .header("Authorization", bearer(memberToken)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
