package com.qianxun.homestay.modules.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.common.security.service.JwtService;
import com.qianxun.homestay.modules.payment.config.WechatPayProperties;
import com.qianxun.homestay.modules.payment.provider.WechatPayGateway;
import com.qianxun.homestay.modules.payment.service.PaymentTimeoutService;
import com.qianxun.homestay.modules.payment.service.PaymentTransactionService;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.payments.model.TransactionAmount;
import com.wechat.pay.java.service.payments.model.TransactionPayer;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 使用真实 MySQL 8 验证 P01/P08 预订支付闭环、快照、幂等和超时状态。 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "app.jwt.member-expiration-seconds=2592000",
        "app.jwt.admin-expiration-seconds=2592000",
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
    @Autowired
    private WechatPayProperties wechatPayProperties;
    @Autowired
    private PaymentTransactionService paymentTransactionService;
    @Autowired
    private PaymentTimeoutService paymentTimeoutService;
    @MockitoBean
    private WechatPayGateway wechatPayGateway;

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
        jdbcTemplate.update("DELETE FROM holiday_price_date");
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

        assertThat(prepay.path("payment_method").asText()).isEqualTo("MOCK");
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
    void holidayPricesAffectNewQuotesAndPreserveLockedAndCompletedAmounts() throws Exception {
        String request = prepayRequestBody();
        JsonNode dates = objectMapper.readTree(request);
        LocalDate checkIn = LocalDate.parse(dates.path("check_in").asText());
        LocalDate checkOut = LocalDate.parse(dates.path("check_out").asText());
        jdbcTemplate.update("UPDATE room SET holiday_price = 180.00 WHERE id = ?", roomId);
        jdbcTemplate.update("INSERT INTO holiday_price_date (holiday_date) VALUES (?)", checkIn.plusDays(1));

        String preview = objectMapper.writeValueAsString(Map.of(
                "room_id", roomId, "check_in", checkIn, "check_out", checkOut, "points_used", POINTS_TO_USE));
        mockMvc.perform(post("/api/app/points/preview").header("Authorization", bearer(memberToken))
                        .contentType(MediaType.APPLICATION_JSON).content(preview))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.room_amount").value("280.00"))
                .andExpect(jsonPath("$.data.pay_amount").value("270.00"));

        JsonNode prepay = prepay(request);
        assertThat(prepay.path("room_amount").asText()).isEqualTo("280.00");
        assertThat(prepay.path("pay_amount").asText()).isEqualTo("270.00");
        String paymentNo = prepay.path("payment_no").asText();

        jdbcTemplate.update("UPDATE room SET price = 130.00, holiday_price = 250.00 WHERE id = ?", roomId);
        jdbcTemplate.update("DELETE FROM holiday_price_date WHERE holiday_date = ?", checkIn.plusDays(1));
        String requote = objectMapper.writeValueAsString(Map.of(
                "room_id", roomId, "check_in", checkIn, "check_out", checkOut));
        mockMvc.perform(post("/api/app/rooms/availability-check").contentType(MediaType.APPLICATION_JSON).content(requote))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.room_amount").value("260.00"));

        JsonNode result = mockPay(paymentNo).path("data");
        assertThat(result.path("paid_amount").asText()).isEqualTo("270.00");
        assertThat(mockPay(paymentNo).path("data").path("order_id").asLong()).isEqualTo(result.path("order_id").asLong());
        mockMvc.perform(get("/api/app/orders/{id}", result.path("order_id").asLong())
                        .header("Authorization", bearer(memberToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.room_amount").value("280.00"))
                .andExpect(jsonPath("$.data.paid_amount").value("270.00"));
    }

    @Test
    void enabledSwitchCreatesWechatTradeAndRejectsMockPayment() throws Exception {
        wechatPayProperties.setEnabled(true);
        try {
            JsonNode prepay = prepay();
            assertThat(prepay.path("payment_method").asText()).isEqualTo("WECHAT");
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT payment_method FROM payments WHERE payment_no = ?", String.class,
                    prepay.path("payment_no").asText())).isEqualTo("WECHAT");

            mockMvc.perform(post("/api/app/payments/{paymentNo}/mock-pay", prepay.path("payment_no").asText())
                            .header("Authorization", bearer(memberToken)))
                    .andExpect(status().isConflict());
        } finally {
            wechatPayProperties.setEnabled(false);
        }
    }

    @Test
    void disabledSwitchRejectsWechatPrepayWithoutCallingWechat() throws Exception {
        assertThat(wechatPayProperties.isEnabled()).isFalse();
        JsonNode prepay = prepay();
        mockMvc.perform(post("/api/app/payments/{paymentNo}/wechat-prepay", prepay.path("payment_no").asText())
                        .header("Authorization", bearer(memberToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login_code\":\"unused\",\"phone_code\":\"unused\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void wechatPrepayEndpointValidatesOneTimeCodesAndRequiresMemberAuthentication() throws Exception {
        JsonNode prepay = prepay();
        String paymentNo = prepay.path("payment_no").asText();

        mockMvc.perform(post("/api/app/payments/{paymentNo}/wechat-prepay", paymentNo)
                        .header("Authorization", bearer(memberToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login_code\":\"\",\"phone_code\":\"phone-code\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/app/payments/{paymentNo}/wechat-prepay", paymentNo)
                        .header("Authorization", bearer(memberToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login_code\":{\"code\":\"invalid-type\"},\"phone_code\":\"phone-code\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/app/payments/{paymentNo}/wechat-prepay", paymentNo)
                        .header("Authorization", bearer(memberToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login_code\":\"" + "x".repeat(129) + "\",\"phone_code\":\"phone-code\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/app/payments/{paymentNo}/wechat-prepay", paymentNo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login_code\":\"login-code\",\"phone_code\":\"phone-code\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void verifiedWechatSettlementCreatesOneOrderAndChecksPayerAndAmount() throws Exception {
        wechatPayProperties.setEnabled(true);
        try {
            JsonNode prepay = prepay();
            String paymentNo = prepay.path("payment_no").asText();
            jdbcTemplate.update("UPDATE members SET openid = 'wx-test-openid' WHERE id = ?", memberId);
            LocalDateTime paidAt = LocalDateTime.now(BUSINESS_ZONE);

            assertThatThrownBy(() -> paymentTransactionService.settleWechatPayment(
                    paymentNo, "wx-test-transaction", new BigDecimal("189.99"), paidAt, "wx-test-openid"))
                    .isInstanceOf(BusinessException.class);
            assertThatThrownBy(() -> paymentTransactionService.settleWechatPayment(
                    paymentNo, "wx-test-transaction", new BigDecimal("190.00"), paidAt, "wrong-openid"))
                    .isInstanceOf(BusinessException.class);
            assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders", Integer.class)).isZero();

            var first = paymentTransactionService.settleWechatPayment(
                    paymentNo, "wx-test-transaction", new BigDecimal("190.00"), paidAt, "wx-test-openid");
            var duplicate = paymentTransactionService.settleWechatPayment(
                    paymentNo, "wx-test-transaction", new BigDecimal("190.00"), paidAt, "wx-test-openid");

            assertThat(first.payment().orderId()).isEqualTo(duplicate.payment().orderId());
            assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders", Integer.class)).isEqualTo(1);
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT transaction_id FROM payments WHERE payment_no = ?", String.class, paymentNo))
                    .isEqualTo("wx-test-transaction");
        } finally {
            wechatPayProperties.setEnabled(false);
        }
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

    @Test
    void expiredWechatLockIsReleasedOnlyAfterProviderCloseSucceeds() throws Exception {
        JsonNode prepay = wechatPrepay();
        String paymentNo = prepay.path("payment_no").asText();
        long paymentId = expireWechatTrade(prepay);

        paymentTimeoutService.closeExpiredPayment(paymentId);
        paymentTimeoutService.closeExpiredPayment(paymentId);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM payments WHERE id = ?", String.class, paymentId)).isEqualTo("CLOSED");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM booking_lock WHERE id = ?", String.class,
                prepay.path("booking_id").asLong())).isEqualTo("EXPIRED");
        verify(wechatPayGateway, times(1)).closePayment(paymentNo);
    }

    @Test
    void closeFailureWithWechatClosedQueryReleasesReservation() throws Exception {
        JsonNode prepay = wechatPrepay();
        String paymentNo = prepay.path("payment_no").asText();
        long paymentId = expireWechatTrade(prepay);
        doThrow(new IllegalStateException("close response lost"))
                .when(wechatPayGateway).closePayment(paymentNo);
        Transaction transaction = new Transaction();
        transaction.setTradeState(Transaction.TradeStateEnum.CLOSED);
        when(wechatPayGateway.queryPayment(paymentNo)).thenReturn(transaction);

        paymentTimeoutService.closeExpiredPayment(paymentId);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM payments WHERE id = ?", String.class, paymentId)).isEqualTo("CLOSED");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM booking_lock WHERE id = ?", String.class,
                prepay.path("booking_id").asLong())).isEqualTo("EXPIRED");
    }

    @Test
    void unknownWechatTradeStateKeepsPaymentAndRoomReservationPending() throws Exception {
        JsonNode prepay = wechatPrepay();
        String paymentNo = prepay.path("payment_no").asText();
        long paymentId = expireWechatTrade(prepay);
        doThrow(new IllegalStateException("close response lost"))
                .when(wechatPayGateway).closePayment(paymentNo);
        Transaction transaction = new Transaction();
        transaction.setTradeState(Transaction.TradeStateEnum.NOTPAY);
        when(wechatPayGateway.queryPayment(paymentNo)).thenReturn(transaction);

        paymentTimeoutService.closeExpiredPayment(paymentId);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM payments WHERE id = ?", String.class, paymentId)).isEqualTo("PENDING");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM booking_lock WHERE id = ?", String.class,
                prepay.path("booking_id").asLong())).isEqualTo("PENDING");
    }

    @Test
    void closeAndQueryNetworkFailuresKeepWechatReservationPending() throws Exception {
        JsonNode prepay = wechatPrepay();
        String paymentNo = prepay.path("payment_no").asText();
        long paymentId = expireWechatTrade(prepay);
        doThrow(new IllegalStateException("close unavailable"))
                .when(wechatPayGateway).closePayment(paymentNo);
        when(wechatPayGateway.queryPayment(paymentNo)).thenThrow(new IllegalStateException("query unavailable"));

        paymentTimeoutService.closeExpiredPayment(paymentId);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM payments WHERE id = ?", String.class, paymentId)).isEqualTo("PENDING");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM booking_lock WHERE id = ?", String.class,
                prepay.path("booking_id").asLong())).isEqualTo("PENDING");
    }

    @Test
    void closeFailureFollowedByWechatSuccessQuerySettlesPaidOrderInsteadOfReleasingRoom() throws Exception {
        JsonNode prepay = wechatPrepay();
        String paymentNo = prepay.path("payment_no").asText();
        long paymentId = expireWechatTrade(prepay);
        String openid = "wx-timeout-test-openid";
        jdbcTemplate.update("UPDATE members SET openid = ? WHERE id = ?", openid, memberId);
        LocalDateTime expiresAt = LocalDateTime.now(BUSINESS_ZONE).minusMinutes(1);
        LocalDateTime paidAt = expiresAt.minusMinutes(1);
        doThrow(new IllegalStateException("close raced with payment"))
                .when(wechatPayGateway).closePayment(paymentNo);
        Transaction transaction = new Transaction();
        transaction.setTradeState(Transaction.TradeStateEnum.SUCCESS);
        transaction.setOutTradeNo(paymentNo);
        transaction.setTransactionId("wx-timeout-transaction");
        transaction.setSuccessTime(paidAt.atZone(BUSINESS_ZONE).toOffsetDateTime().toString());
        TransactionAmount amount = new TransactionAmount();
        amount.setTotal(19000);
        amount.setCurrency("CNY");
        transaction.setAmount(amount);
        TransactionPayer payer = new TransactionPayer();
        payer.setOpenid(openid);
        transaction.setPayer(payer);
        when(wechatPayGateway.queryPayment(paymentNo)).thenReturn(transaction);

        paymentTimeoutService.closeExpiredPayment(paymentId);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM payments WHERE id = ?", String.class, paymentId)).isEqualTo("SUCCESS");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM booking_lock WHERE id = ?", String.class,
                prepay.path("booking_id").asLong())).isEqualTo("PAID");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders", Integer.class)).isEqualTo(1);
    }

    private JsonNode prepay() throws Exception {
        return prepay(prepayRequestBody());
    }

    private JsonNode prepay(String request) throws Exception {
        String response = mockMvc.perform(post("/api/app/payments/prepay")
                        .header("Authorization", bearer(memberToken))
                        .contentType("application/json")
                        .content(request))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data");
    }

    private JsonNode wechatPrepay() throws Exception {
        wechatPayProperties.setEnabled(true);
        try {
            return prepay();
        } finally {
            wechatPayProperties.setEnabled(false);
        }
    }

    private long expireWechatTrade(JsonNode prepay) {
        String paymentNo = prepay.path("payment_no").asText();
        Long paymentId = jdbcTemplate.queryForObject(
                "SELECT id FROM payments WHERE payment_no = ?", Long.class, paymentNo);
        jdbcTemplate.update("UPDATE payments SET wechat_prepay_id = 'test-prepay-id' WHERE id = ?", paymentId);
        jdbcTemplate.update("UPDATE booking_lock SET expires_at = ? WHERE id = ?",
                LocalDateTime.now(BUSINESS_ZONE).minusMinutes(1), prepay.path("booking_id").asLong());
        return paymentId;
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
