package com.qianxun.homestay.modules.mall;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.qianxun.homestay.modules.booking.mapper.BookingLockMapper;
import com.qianxun.homestay.modules.booking.dto.PrepayRequest;
import com.qianxun.homestay.modules.booking.service.BookingLockService;
import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.modules.mall.mapper.MallPaymentReservationMapper;
import com.qianxun.homestay.modules.mall.service.MallOrderService;
import com.qianxun.homestay.modules.mall.service.MallPaymentService;
import com.qianxun.homestay.modules.mall.service.MallRefundService;
import com.qianxun.homestay.modules.mall.service.MallWechatRefundTransactionService;
import com.qianxun.homestay.modules.mall.service.MallPaymentTimeoutService;
import com.qianxun.homestay.modules.mall.mapper.MallPaymentMapper;
import com.qianxun.homestay.modules.payment.config.WechatPayProperties;
import com.qianxun.homestay.common.security.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qianxun.homestay.modules.mall.dto.MallShipRequest;
import com.qianxun.homestay.modules.mall.dto.MallPrepayRequest;
import com.qianxun.homestay.modules.mall.dto.MallPreviewRequest;
import com.qianxun.homestay.modules.order.domain.OrderNoGenerator;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** 使用真实 MySQL 验证两种业务的积分预占释放条件。 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "app.channel.mini-program-id=1"
})
@AutoConfigureMockMvc
class MallPointsReservationIntegrationTest {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter ORDER_NO_DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("mall_points_test").withUsername("qianxun").withPassword("qianxun-test-password");

    @Autowired private JdbcTemplate jdbc;
    @Autowired private MallPaymentReservationMapper mallReservations;
    @Autowired private BookingLockMapper bookingReservations;
    @Autowired private BookingLockService bookingService;
    @Autowired private MallOrderService mallOrderService;
    @Autowired private MallPaymentService mallPaymentService;
    @Autowired private MallRefundService mallRefundService;
    @Autowired private MallWechatRefundTransactionService mallWechatRefundTransactions;
    @Autowired private MallPaymentTimeoutService mallTimeout;
    @Autowired private MallPaymentMapper mallPayments;
    @Autowired private WechatPayProperties wechatProperties;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private OrderNoGenerator orderNoGenerator;
    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Test
    void expiredUnresolvedWechatTransactionsRetainReservedPoints() {
        jdbc.update("INSERT INTO members (phone) VALUES ('13800000981')");
        long memberId = jdbc.queryForObject("SELECT id FROM members WHERE phone = '13800000981'", Long.class);
        long productId = jdbc.queryForObject("SELECT id FROM mall_product LIMIT 1", Long.class);
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
        for (String method : new String[] {"MOCK", "WECHAT"}) {
            jdbc.update("""
                    INSERT INTO mall_payment (payment_no, member_id, product_id, product_name, quantity,
                        total_amount, points_used, amount, recipient_name, recipient_phone, shipping_address,
                        payment_method, status, expires_at)
                    VALUES (?, ?, ?, '测试商品', 1, 100, 20, 99, '收货人', '13800000981', '测试地址', ?, 'PENDING', ?)
                    """, "mall-" + method, memberId, productId, method, now.minusMinutes(1));
        }
        assertThat(mallReservations.sumReservedPoints(memberId, now)).isEqualTo(20);

        jdbc.update("INSERT INTO room (number, name, floor, price, capacity, enabled) VALUES ('981', '测试房', 1, 100, 2, 1)");
        long roomId = jdbc.queryForObject("SELECT id FROM room WHERE number = '981'", Long.class);
        jdbc.update("""
                INSERT INTO booking_lock (member_id, room_id, check_in, check_out, guest_name, phone, guest_count,
                    room_amount, discount_amount, points_used, amount, status, expires_at)
                VALUES (?, ?, '2027-01-01', '2027-01-02', '客人', '13800000981', 1, 100, 1, 30, 99, 'PENDING', ?)
                """, memberId, roomId, now.minusMinutes(1));
        long bookingId = jdbc.queryForObject("SELECT id FROM booking_lock WHERE member_id = ?", Long.class, memberId);
        jdbc.update("INSERT INTO payments (booking_id, payment_no, payment_method, amount, status) VALUES (?, 'room-wechat', 'WECHAT', 99, 'PENDING')", bookingId);
        assertThat(bookingReservations.sumReservedPoints(memberId, now)).isEqualTo(30);
        jdbc.update("UPDATE payments SET status = 'CLOSED' WHERE payment_no = 'room-wechat'");
        assertThat(bookingReservations.sumReservedPoints(memberId, now)).isZero();
    }

    @Test
    void bookingAfterConcurrentMallReservationCannotReusePoints() throws Exception {
        jdbc.update("INSERT INTO members (phone, points) VALUES ('13800000982', 300)");
        long memberId = jdbc.queryForObject("SELECT id FROM members WHERE phone = '13800000982'", Long.class);
        long productId = jdbc.queryForObject("SELECT id FROM mall_product LIMIT 1", Long.class);
        jdbc.update("INSERT INTO room (number, name, floor, price, capacity, enabled) VALUES ('982', '积分测试房', 1, 100, 2, 1)");
        long roomId = jdbc.queryForObject("SELECT id FROM room WHERE number = '982'", Long.class);
        CountDownLatch reservationInserted = new CountDownLatch(1);
        CountDownLatch releaseReservation = new CountDownLatch(1);
        CountDownLatch bookingStarted = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var mall = executor.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                jdbc.queryForObject("SELECT id FROM members WHERE id = ? FOR UPDATE", Long.class, memberId);
                jdbc.update("""
                        INSERT INTO mall_payment (payment_no, member_id, product_id, product_name, quantity,
                            total_amount, points_used, amount, recipient_name, recipient_phone, shipping_address,
                            payment_method, status, expires_at)
                        VALUES ('concurrent-mall', ?, ?, '商品', 1, 100, 200, 93.34, '收货人', '13800000982', '地址', 'MOCK', 'PENDING', ?)
                        """, memberId, productId, LocalDateTime.now(ZoneId.of("Asia/Shanghai")).plusMinutes(10));
                reservationInserted.countDown();
                try {
                    if (!releaseReservation.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("等待超时");
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(exception);
                }
            }));
            assertThat(reservationInserted.await(10, TimeUnit.SECONDS)).isTrue();
            var booking = executor.submit(() -> {
                bookingStarted.countDown();
                LocalDate checkIn = LocalDate.now(ZoneId.of("Asia/Shanghai")).plusDays(2);
                try {
                    bookingService.prepay(memberId, new PrepayRequest(roomId, checkIn, checkIn.plusDays(1),
                            "客人", "13800000982", 1, null, 200));
                    return "未拒绝重复预占";
                } catch (BusinessException exception) {
                    return exception.getMessage();
                }
            });
            assertThat(bookingStarted.await(10, TimeUnit.SECONDS)).isTrue();
            releaseReservation.countDown();
            mall.get(15, TimeUnit.SECONDS);
            assertThat(booking.get(15, TimeUnit.SECONDS)).contains("抵扣积分超过当前可用积分");
        } finally {
            releaseReservation.countDown();
        }
    }

    @Test
    void shippingAndRepeatedCompletionRewardThePurchaserOnlyOnce() {
        jdbc.update("INSERT INTO members (phone, points, growth_points) VALUES ('13800000983', 10, 20)");
        long memberId = jdbc.queryForObject("SELECT id FROM members WHERE phone = '13800000983'", Long.class);
        long productId = jdbc.queryForObject("SELECT id FROM mall_product LIMIT 1", Long.class);
        jdbc.update("""
                INSERT INTO mall_order (order_no, member_id, product_id, product_name, quantity, total_amount,
                    paid_amount, points_used, recipient_name, recipient_phone, shipping_address, status, paid_at)
                VALUES ('mall-order-reward-test', ?, ?, '蚕丝被', 1, 100, 99.90, 0, '收货人', '13800000983',
                    '收货地址', 'PENDING_SHIPMENT', CURRENT_TIMESTAMP(3))
                """, memberId, productId);
        long orderId = jdbc.queryForObject("SELECT id FROM mall_order WHERE order_no = 'mall-order-reward-test'", Long.class);
        assertThat(mallOrderService.ship(orderId, new MallShipRequest("测试快递", "123456")).status()).isEqualTo("SHIPPED");
        assertThat(mallOrderService.complete(orderId).status()).isEqualTo("COMPLETED");
        assertThat(mallOrderService.complete(orderId).status()).isEqualTo("COMPLETED");
        assertThat(jdbc.queryForObject("SELECT points FROM members WHERE id = ?", Integer.class, memberId)).isEqualTo(109);
        assertThat(jdbc.queryForObject("SELECT growth_points FROM members WHERE id = ?", Integer.class, memberId)).isEqualTo(119);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM points_log WHERE mall_order_id = ?", Integer.class, orderId)).isEqualTo(1);
        assertThat(mallOrderService.findMemberPage(memberId, 1, 20).total()).isEqualTo(1);
        assertThat(mallOrderService.findMemberPage(memberId + 10000, 1, 20).total()).isZero();
    }

    @Test
    void concurrentMockPaymentsCreateOneOrderAndDeductPointsOnce() throws Exception {
        jdbc.update("INSERT INTO members (phone, points) VALUES ('13800000984', 300)");
        long memberId = jdbc.queryForObject("SELECT id FROM members WHERE phone = '13800000984'", Long.class);
        jdbc.update("INSERT INTO mall_product (name, description, images, price, status) VALUES ('并发商品', '描述', JSON_ARRAY(), 100, 'ON_SHELF')");
        long productId = jdbc.queryForObject("SELECT id FROM mall_product WHERE name = '并发商品'", Long.class);
        var preview = mallPaymentService.preview(memberId, new MallPreviewRequest(productId, 1, 200));
        assertThat(preview.amount()).isEqualTo("93.34");
        assertThat(mallPaymentService.shipping(memberId).shippingAddress()).isNull();
        var prepay = mallPaymentService.prepay(memberId,
                new MallPrepayRequest(productId, 1, 200, "收货人", "13800000984", "收货地址"));
        assertThat(jdbc.queryForObject("SELECT points FROM members WHERE id = ?", Integer.class, memberId)).isEqualTo(300);
        CyclicBarrier start = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> { start.await(); return mallPaymentService.mockPay(memberId, prepay.paymentNo()); });
            var second = executor.submit(() -> { start.await(); return mallPaymentService.mockPay(memberId, prepay.paymentNo()); });
            var firstResult = first.get(15, TimeUnit.SECONDS);
            var secondResult = second.get(15, TimeUnit.SECONDS);
            assertThat(firstResult.orderId()).isEqualTo(secondResult.orderId());
            assertThat(firstResult.orderNo()).matches(todayOrderNoPattern());
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM mall_order WHERE member_id = ?", Integer.class, memberId)).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT points FROM members WHERE id = ?", Integer.class, memberId)).isEqualTo(100);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM points_log WHERE mall_order_id = ? AND type = 'REDEEM'", Integer.class,
                    firstResult.orderId())).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT default_shipping_address FROM members WHERE id = ?", String.class, memberId)).isEqualTo("收货地址");
            assertThat(mallPaymentService.shipping(memberId).recipientPhone()).isEqualTo("13800000984");
            assertThat(mallPaymentService.shipping(memberId).shippingAddress()).isEqualTo("收货地址");
            var refund = mallRefundService.refund(memberId, firstResult.orderId());
            assertThat(mallRefundService.refund(memberId, firstResult.orderId()).refundNo()).isEqualTo(refund.refundNo());
            assertThat(jdbc.queryForObject("SELECT points FROM members WHERE id = ?", Integer.class, memberId)).isEqualTo(300);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM mall_refund WHERE mall_order_id = ?", Integer.class, firstResult.orderId())).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM points_log WHERE mall_order_id = ? AND type = 'RETURN'", Integer.class,
                    firstResult.orderId())).isEqualTo(1);
        }
    }

    @Test
    void roomAndMallOrdersShareUniqueDailyNumbersDuringConcurrentCreation() throws Exception {
        jdbc.update("INSERT INTO members (phone) VALUES ('13800000986')");
        long memberId = jdbc.queryForObject("SELECT id FROM members WHERE phone = '13800000986'", Long.class);
        long productId = jdbc.queryForObject("SELECT id FROM mall_product LIMIT 1", Long.class);
        jdbc.update("INSERT INTO room (number, name, floor, price, capacity, enabled) VALUES ('986', '编号测试房', 1, 100, 2, 1)");
        long roomId = jdbc.queryForObject("SELECT id FROM room WHERE number = '986'", Long.class);
        CyclicBarrier start = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var roomOrder = executor.submit(() -> {
                start.await();
                return new TransactionTemplate(transactionManager).execute(status -> {
                    String number = orderNoGenerator.nextOrderNo();
                    jdbc.update("INSERT INTO orders (order_no, room_id, channel_id, check_in, check_out, status) "
                            + "VALUES (?, ?, 1, '2027-01-01', '2027-01-02', 'PENDING_CONFIRM')", number, roomId);
                    return number;
                });
            });
            var mallOrder = executor.submit(() -> {
                start.await();
                return new TransactionTemplate(transactionManager).execute(status -> {
                    String number = orderNoGenerator.nextOrderNo();
                    jdbc.update("""
                            INSERT INTO mall_order (order_no, member_id, product_id, product_name, quantity, total_amount,
                                paid_amount, points_used, recipient_name, recipient_phone, shipping_address, status, paid_at)
                            VALUES (?, ?, ?, '商品', 1, 1, 1, 0, '收货人', '13800000986', '收货地址',
                                'PENDING_SHIPMENT', CURRENT_TIMESTAMP(3))
                            """, number, memberId, productId);
                    return number;
                });
            });
            String roomNumber = roomOrder.get(15, TimeUnit.SECONDS);
            String mallNumber = mallOrder.get(15, TimeUnit.SECONDS);
            assertThat(roomNumber).matches(todayOrderNoPattern());
            assertThat(mallNumber).matches(todayOrderNoPattern()).isNotEqualTo(roomNumber);
            assertThat(Math.abs(orderNoSequence(roomNumber) - orderNoSequence(mallNumber))).isEqualTo(1L);
        }
    }

    @Test
    void numberGenerationReadsCommittedMallOrdersAfterAnEarlierTransactionSnapshot() throws Exception {
        jdbc.update("INSERT INTO members (phone) VALUES ('13800000987')");
        long memberId = jdbc.queryForObject("SELECT id FROM members WHERE phone = '13800000987'", Long.class);
        long productId = jdbc.queryForObject("SELECT id FROM mall_product LIMIT 1", Long.class);
        CountDownLatch snapshotCreated = new CountDownLatch(1);
        CountDownLatch otherCommitted = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> new TransactionTemplate(transactionManager).execute(status -> {
                jdbc.queryForObject("SELECT COUNT(*) FROM orders", Long.class);
                snapshotCreated.countDown();
                try {
                    if (!otherCommitted.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("等待超时");
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(exception);
                }
                return orderNoGenerator.nextOrderNo();
            }));
            assertThat(snapshotCreated.await(10, TimeUnit.SECONDS)).isTrue();
            String inserted = new TransactionTemplate(transactionManager).execute(status -> {
                String number = orderNoGenerator.nextOrderNo();
                jdbc.update("""
                        INSERT INTO mall_order (order_no, member_id, product_id, product_name, quantity, total_amount,
                            paid_amount, points_used, recipient_name, recipient_phone, shipping_address, status, paid_at)
                        VALUES (?, ?, ?, '商品', 1, 1, 1, 0, '收货人', '13800000987', '地址',
                            'PENDING_SHIPMENT', CURRENT_TIMESTAMP(3))
                        """, number, memberId, productId);
                return number;
            });
            otherCommitted.countDown();
            assertThat(first.get(15, TimeUnit.SECONDS)).isNotEqualTo(inserted);
        } finally {
            otherCommitted.countDown();
        }
    }

    @Test
    void wechatSettlementChecksPayerAndIsIdempotent() throws Exception {
        jdbc.update("INSERT INTO members (phone, openid, points) VALUES ('13800000985', 'mall-openid-test', 300)");
        long memberId = jdbc.queryForObject("SELECT id FROM members WHERE phone = '13800000985'", Long.class);
        long productId = jdbc.queryForObject("SELECT id FROM mall_product LIMIT 1", Long.class);
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Shanghai")).withNano(0);
        jdbc.update("""
                INSERT INTO mall_payment (payment_no, member_id, product_id, product_name, quantity,
                    total_amount, points_used, amount, recipient_name, recipient_phone, shipping_address,
                    payment_method, status, expires_at)
                VALUES ('M-wechat-test', ?, ?, '商品', 1, 100, 0, 100, '收货人', '13800000985', '地址', 'WECHAT', 'PENDING', ?)
                """, memberId, productId, now.plusMinutes(10));
        assertThatThrownBy(() -> mallPaymentService.settleWechat("M-wechat-test", "wx-transaction-1",
                new BigDecimal("100.00"), now, "wrong-openid")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> mallPaymentService.settleWechat("M-wechat-test", "wx-transaction-1",
                new BigDecimal("99.99"), now, "mall-openid-test")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> mallPaymentService.settleWechat("M-wechat-test", "wx-transaction-1",
                new BigDecimal("100.00"), now.plusMinutes(11), "mall-openid-test")).isInstanceOf(BusinessException.class);
        try (var executor = Executors.newFixedThreadPool(2)) {
            CyclicBarrier start = new CyclicBarrier(2);
            var first = executor.submit(() -> { start.await(); return mallPaymentService.settleWechat("M-wechat-test",
                    "wx-transaction-1", new BigDecimal("100.00"), now, "mall-openid-test"); });
            var second = executor.submit(() -> { start.await(); return mallPaymentService.settleWechat("M-wechat-test",
                    "wx-transaction-1", new BigDecimal("100.00"), now, "mall-openid-test"); });
            assertThat(second.get(15, TimeUnit.SECONDS).orderId()).isEqualTo(first.get(15, TimeUnit.SECONDS).orderId());
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM mall_order WHERE member_id = ?", Integer.class, memberId)).isEqualTo(1);
        assertThatThrownBy(() -> mallPaymentService.settleWechat("M-wechat-test", "wx-transaction-other",
                new BigDecimal("100.00"), now, "mall-openid-test")).isInstanceOf(BusinessException.class);
    }

    @Test
    void wechatRefundRemainsPendingUntilConfirmedAndReturnsPointsOnce() throws Exception {
        jdbc.update("INSERT INTO members (phone, openid, points) VALUES ('13800000988', 'mall-refund-openid', 300)");
        long memberId = jdbc.queryForObject("SELECT id FROM members WHERE phone = '13800000988'", Long.class);
        long productId = jdbc.queryForObject("SELECT id FROM mall_product LIMIT 1", Long.class);
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Shanghai")).withNano(0);
        jdbc.update("""
                INSERT INTO mall_payment (payment_no, member_id, product_id, product_name, quantity, total_amount,
                    points_used, amount, recipient_name, recipient_phone, shipping_address, payment_method, status, expires_at)
                VALUES ('M-refund-test', ?, ?, '商品', 1, 100, 200, 93.34, '收货人', '13800000988', '地址', 'WECHAT', 'PENDING', ?)
                """, memberId, productId, now.plusMinutes(10));
        var paid = mallPaymentService.settleWechat("M-refund-test", "wx-refund-payment", new BigDecimal("93.34"), now, "mall-refund-openid");
        var prepared = mallWechatRefundTransactions.prepare(memberId, paid.orderId());
        assertThat(prepared.status()).isEqualTo("PENDING");
        assertThat(mallWechatRefundTransactions.prepare(memberId, paid.orderId()).refundNo()).isEqualTo(prepared.refundNo());
        assertThatThrownBy(() -> mallOrderService.ship(paid.orderId(), new MallShipRequest("快递", "单号")))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> mallWechatRefundTransactions.complete(prepared.refundNo(), "M-refund-test", "wrong",
                new BigDecimal("93.34"), new BigDecimal("93.34"), "wx-refund-id")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> mallWechatRefundTransactions.complete(prepared.refundNo(), "M-refund-test", "wx-refund-payment",
                new BigDecimal("93.33"), new BigDecimal("93.34"), "wx-refund-id")).isInstanceOf(BusinessException.class);
        assertThat(jdbc.queryForObject("SELECT points FROM members WHERE id = ?", Integer.class, memberId)).isEqualTo(100);
        try (var executor = Executors.newFixedThreadPool(2)) {
            CyclicBarrier start = new CyclicBarrier(2);
            var first = executor.submit(() -> { start.await(); return mallWechatRefundTransactions.complete(prepared.refundNo(),
                    "M-refund-test", "wx-refund-payment", new BigDecimal("93.34"), new BigDecimal("93.34"), "wx-refund-id"); });
            var second = executor.submit(() -> { start.await(); return mallWechatRefundTransactions.complete(prepared.refundNo(),
                    "M-refund-test", "wx-refund-payment", new BigDecimal("93.34"), new BigDecimal("93.34"), "wx-refund-id"); });
            assertThat(first.get(15, TimeUnit.SECONDS).status()).isEqualTo("SUCCESS");
            assertThat(second.get(15, TimeUnit.SECONDS).status()).isEqualTo("SUCCESS");
        }
        assertThat(jdbc.queryForObject("SELECT points FROM members WHERE id = ?", Integer.class, memberId)).isEqualTo(300);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM points_log WHERE mall_order_id = ? AND type = 'RETURN'",
                Integer.class, paid.orderId())).isEqualTo(1);
    }

    @Test
    void configuredWechatModeKeepsUncertainPrepayReservedUntilChannelIsKnown() {
        jdbc.update("INSERT INTO members (phone, points) VALUES ('13800000989', 300)");
        long memberId = jdbc.queryForObject("SELECT id FROM members WHERE phone = '13800000989'", Long.class);
        jdbc.update("INSERT INTO mall_product (name, description, images, price, status) VALUES ('微信预支付测试商品', '描述', JSON_ARRAY(), 100, 'ON_SHELF')");
        long productId = jdbc.queryForObject("SELECT id FROM mall_product WHERE name = '微信预支付测试商品'", Long.class);
        wechatProperties.setEnabled(true);
        try {
            var prepay = mallPaymentService.prepay(memberId,
                    new MallPrepayRequest(productId, 1, 200, "收货人", "13800000989", "地址"));
            assertThat(prepay.paymentMethod()).isEqualTo("WECHAT");
            assertThat(prepay.paymentNo()).hasSize(32);
            assertThatThrownBy(() -> mallPaymentService.mockPay(memberId, prepay.paymentNo())).isInstanceOf(BusinessException.class);
            var payment = mallPayments.findByPaymentNo(prepay.paymentNo());
            assertThat(mallPayments.claimWechatPrepay(payment.getId(), LocalDateTime.now(ZoneId.of("Asia/Shanghai")))).isEqualTo(1);
            jdbc.update("UPDATE mall_payment SET expires_at = ? WHERE id = ?",
                    LocalDateTime.now(ZoneId.of("Asia/Shanghai")).minusMinutes(1), payment.getId());
            mallTimeout.closeExpired(payment.getId());
            assertThat(mallPayments.findByPaymentNo(prepay.paymentNo()).getStatus()).isEqualTo("PENDING");
            assertThat(mallReservations.sumReservedPoints(memberId, LocalDateTime.now(ZoneId.of("Asia/Shanghai")))).isEqualTo(200);
        } finally { wechatProperties.setEnabled(false); }
    }

    @Test
    void expiredMockPaymentCannotCreateAnOrder() {
        jdbc.update("INSERT INTO members (phone, points) VALUES ('13800000991', 300)");
        long memberId = jdbc.queryForObject("SELECT id FROM members WHERE phone = '13800000991'", Long.class);
        long productId = jdbc.queryForObject("SELECT id FROM mall_product LIMIT 1", Long.class);
        jdbc.update("""
                INSERT INTO mall_payment (payment_no, member_id, product_id, product_name, quantity, total_amount,
                    points_used, amount, recipient_name, recipient_phone, shipping_address, payment_method, status, expires_at)
                VALUES ('M-expired-test', ?, ?, '商品', 1, 100, 200, 93.34, '收货人', '13800000991', '地址', 'MOCK', 'PENDING', ?)
                """, memberId, productId, LocalDateTime.now(ZoneId.of("Asia/Shanghai")).minusMinutes(1));
        var payment = mallPayments.findByPaymentNo("M-expired-test");
        mallTimeout.closeExpired(payment.getId());
        assertThat(mallPayments.findByPaymentNo("M-expired-test").getStatus()).isEqualTo("CLOSED");
        assertThatThrownBy(() -> mallPaymentService.mockPay(memberId, "M-expired-test")).isInstanceOf(BusinessException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM mall_order WHERE member_id = ?", Integer.class, memberId)).isZero();
        assertThat(jdbc.queryForObject("SELECT points FROM members WHERE id = ?", Integer.class, memberId)).isEqualTo(300);
    }

    /** 返回当前北京时间商城与客房订单共用的订单号格式。 */
    private String todayOrderNoPattern() {
        return "QX" + LocalDate.now(BUSINESS_ZONE).format(ORDER_NO_DATE_FORMAT) + "\\d{4,}";
    }

    /** 提取订单号中的日流水，用于验证商城与客房共用连续编号。 */
    private long orderNoSequence(String orderNo) {
        return Long.parseLong(orderNo.substring(10));
    }

    @Test
    void memberCanCompleteMockPurchaseAndRefundThroughApi() throws Exception {
        jdbc.update("INSERT INTO members (phone, points) VALUES ('13800000990', 300)");
        long memberId = jdbc.queryForObject("SELECT id FROM members WHERE phone = '13800000990'", Long.class);
        jdbc.update("INSERT INTO mall_product (name, description, images, price, status) VALUES ('接口测试商品', '描述', JSON_ARRAY(), 100, 'ON_SHELF')");
        long productId = jdbc.queryForObject("SELECT id FROM mall_product WHERE name = '接口测试商品'", Long.class);
        String token = "Bearer " + jwtService.issueMemberToken(memberId);
        mockMvc.perform(get("/api/app/mall/products").param("page", "1").param("size", "20").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(20)).andExpect(jsonPath("$.data.total").isNumber());
        String purchase = "{\"product_id\":" + productId + ",\"quantity\":1,\"points_used\":200,"
                + "\"recipient_name\":\"测试收货人\",\"recipient_phone\":\"13800000990\",\"shipping_address\":\"测试收货地址\"}";
        String response = mockMvc.perform(post("/api/app/mall/payments").header("Authorization", token)
                        .contentType("application/json").content(purchase))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.payment_method").value("MOCK"))
                .andReturn().getResponse().getContentAsString();
        String paymentNo = objectMapper.readTree(response).path("data").path("payment_no").asText();
        String paid = mockMvc.perform(post("/api/app/mall/payments/{paymentNo}/mock-pay", paymentNo).header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andReturn().getResponse().getContentAsString();
        long orderId = objectMapper.readTree(paid).path("data").path("order_id").asLong();
        mockMvc.perform(post("/api/app/mall/payments/{paymentNo}/mock-pay", paymentNo).header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.order_id").value(orderId));
        mockMvc.perform(get("/api/app/mall/orders").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].order_no").isNotEmpty());
        mockMvc.perform(post("/api/app/mall/orders/{id}/refund", orderId).header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("SUCCESS"));
        mockMvc.perform(post("/api/app/mall/orders/{id}/refund", orderId).header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("SUCCESS"));
        mockMvc.perform(get("/api/app/mall/payments/shipping").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.shipping_address").value("测试收货地址"));
        assertThat(jdbc.queryForObject("SELECT points FROM members WHERE id = ?", Integer.class, memberId)).isEqualTo(300);
    }
}
