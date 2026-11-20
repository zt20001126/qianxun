package com.qianxun.homestay.modules.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.modules.auth.service.WechatIdentityService;
import com.qianxun.homestay.modules.booking.entity.BookingLockEntity;
import com.qianxun.homestay.modules.booking.enums.BookingLockStatus;
import com.qianxun.homestay.modules.booking.mapper.BookingLockMapper;
import com.qianxun.homestay.modules.payment.config.WechatPayProperties;
import com.qianxun.homestay.modules.payment.entity.PaymentEntity;
import com.qianxun.homestay.modules.payment.enums.PaymentMethod;
import com.qianxun.homestay.modules.payment.enums.PaymentStatus;
import com.qianxun.homestay.modules.payment.mapper.PaymentMapper;
import com.qianxun.homestay.modules.payment.provider.WechatPayGateway;
import com.qianxun.homestay.modules.payment.service.PaymentTransactionService;
import com.qianxun.homestay.modules.payment.service.impl.WechatPaymentServiceImpl;
import com.qianxun.homestay.modules.refund.service.WechatRefundService;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayWithRequestPaymentResponse;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.payments.model.TransactionAmount;
import com.wechat.pay.java.service.payments.model.TransactionPayer;
import com.wechat.pay.java.service.refund.model.Amount;
import com.wechat.pay.java.service.refund.model.RefundNotification;
import com.wechat.pay.java.service.refund.model.Status;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/** 验证已验签微信通知的字段校验、时区转换与业务分发。 */
class WechatPaymentServiceImplTest {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final String APP_ID = "wx-test-app";
    private static final String MERCHANT_ID = "1900000001";
    private static final String PAYMENT_NO = "PAY-TEST-001";
    private static final String REFUND_NO = "RF-TEST-001";

    private final WechatPayProperties properties = new WechatPayProperties();
    private final WechatPayGateway gateway = mock(WechatPayGateway.class);
    private final PaymentTransactionService paymentTransactionService = mock(PaymentTransactionService.class);
    private final WechatRefundService refundService = mock(WechatRefundService.class);
    private final BookingLockMapper bookingLockMapper = mock(BookingLockMapper.class);
    private final PaymentMapper paymentMapper = mock(PaymentMapper.class);
    private final WechatIdentityService identityService = mock(WechatIdentityService.class);
    private final WechatPaymentServiceImpl service = new WechatPaymentServiceImpl(
            bookingLockMapper, paymentMapper, paymentTransactionService,
            identityService, gateway, properties, refundService);

    @BeforeEach
    void configureMerchantIdentity() {
        properties.setAppId(APP_ID);
        properties.setMerchantId(MERCHANT_ID);
    }

    @Test
    void createsPrepayOnlyForOwnedPendingWechatPaymentAndStoresPrepayId() {
        properties.setEnabled(true);
        PaymentEntity payment = pendingWechatPayment();
        BookingLockEntity booking = pendingBooking(22L);
        when(paymentMapper.findByPaymentNo(PAYMENT_NO)).thenReturn(payment);
        when(bookingLockMapper.findById(booking.getId())).thenReturn(booking);
        when(identityService.verifyAndBindOpenId(22L, "login-code", "phone-code")).thenReturn("wx-openid-022");
        when(gateway.createPrepay(PAYMENT_NO, payment.getAmount(), "wx-openid-022", booking.getExpiresAt()))
                .thenReturn(validPrepayResponse());
        when(paymentMapper.updateWechatPrepayId(payment.getId(), "prepay-001")).thenReturn(1);

        var result = service.createPrepay(22L, PAYMENT_NO, "login-code", "phone-code");

        assertThat(result.appId()).isEqualTo(APP_ID);
        assertThat(result.packageValue()).isEqualTo("prepay_id=prepay-001");
        verify(paymentMapper).updateWechatPrepayId(payment.getId(), "prepay-001");
    }

    @Test
    void refusesAnotherMembersWechatPaymentBeforeIdentityOrProviderCalls() {
        properties.setEnabled(true);
        PaymentEntity payment = pendingWechatPayment();
        BookingLockEntity booking = pendingBooking(99L);
        when(paymentMapper.findByPaymentNo(PAYMENT_NO)).thenReturn(payment);
        when(bookingLockMapper.findById(booking.getId())).thenReturn(booking);

        assertThatThrownBy(() -> service.createPrepay(22L, PAYMENT_NO, "login-code", "phone-code"))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(identityService, never()).verifyAndBindOpenId(any(), any(), any());
        verify(gateway, never()).createPrepay(any(), any(), any(), any());
    }

    @Test
    void expiredWechatPrepayDoesNotExchangeIdentityOrContactProvider() {
        properties.setEnabled(true);
        PaymentEntity payment = pendingWechatPayment();
        BookingLockEntity booking = pendingBooking(22L);
        booking.setExpiresAt(LocalDateTime.now(BUSINESS_ZONE).minusSeconds(1));
        when(paymentMapper.findByPaymentNo(PAYMENT_NO)).thenReturn(payment);
        when(bookingLockMapper.findById(booking.getId())).thenReturn(booking);

        assertThatThrownBy(() -> service.createPrepay(22L, PAYMENT_NO, "login-code", "phone-code"))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(identityService, never()).verifyAndBindOpenId(any(), any(), any());
        verify(gateway, never()).createPrepay(any(), any(), any(), any());
    }

    @Test
    void invalidWechatPackageIsNotPersisted() {
        properties.setEnabled(true);
        PaymentEntity payment = pendingWechatPayment();
        BookingLockEntity booking = pendingBooking(22L);
        PrepayWithRequestPaymentResponse invalidResponse = validPrepayResponse();
        invalidResponse.setPackageVal("wrong-package");
        when(paymentMapper.findByPaymentNo(PAYMENT_NO)).thenReturn(payment);
        when(bookingLockMapper.findById(booking.getId())).thenReturn(booking);
        when(identityService.verifyAndBindOpenId(22L, "login-code", "phone-code")).thenReturn("wx-openid-022");
        when(gateway.createPrepay(PAYMENT_NO, payment.getAmount(), "wx-openid-022", booking.getExpiresAt()))
                .thenReturn(invalidResponse);

        assertThatThrownBy(() -> service.createPrepay(22L, PAYMENT_NO, "login-code", "phone-code"))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getStatus())
                .isEqualTo(HttpStatus.BAD_GATEWAY);

        verify(paymentMapper, never()).updateWechatPrepayId(any(), any());
    }

    @Test
    void validPaymentNotificationConvertsAmountAndTimestampToBeijing() {
        when(gateway.parsePaymentNotification(any(RequestParam.class))).thenReturn(validPayment());

        service.handlePaymentNotification("serial", "timestamp", "nonce", "signature", "raw-body");

        verify(paymentTransactionService).settleWechatPayment(
                PAYMENT_NO, "wx-transaction-001", new BigDecimal("123.45"),
                LocalDateTime.of(2026, 11, 20, 13, 12, 13), "wx-openid-001");
    }

    @Test
    void invalidPaymentNotificationFieldsNeverSettleAnOrder() {
        List<Consumer<Transaction>> invalidCases = List.of(
                transaction -> transaction.setAppid("another-app"),
                transaction -> transaction.setMchid("another-merchant"),
                transaction -> transaction.setTradeState(Transaction.TradeStateEnum.NOTPAY),
                transaction -> transaction.getAmount().setCurrency("USD"),
                transaction -> transaction.setTradeType(Transaction.TradeTypeEnum.NATIVE),
                transaction -> transaction.getAmount().setTotal(null),
                transaction -> transaction.setSuccessTime(null),
                transaction -> transaction.setTransactionId(null),
                transaction -> transaction.setOutTradeNo(null),
                transaction -> transaction.setPayer(null),
                transaction -> transaction.getPayer().setOpenid(null));

        for (Consumer<Transaction> invalidCase : invalidCases) {
            Transaction transaction = validPayment();
            invalidCase.accept(transaction);
            when(gateway.parsePaymentNotification(any(RequestParam.class))).thenReturn(transaction);

            assertThatThrownBy(() -> service.handlePaymentNotification(
                    "serial", "timestamp", "nonce", "signature", "raw-body"))
                    .isInstanceOf(BusinessException.class)
                    .extracting(exception -> ((BusinessException) exception).getStatus())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        verify(paymentTransactionService, never()).settleWechatPayment(
                any(), any(), any(), any(), any());
    }

    @Test
    void paymentNotificationParserFailureDoesNotSettleAnOrder() {
        when(gateway.parsePaymentNotification(any(RequestParam.class)))
                .thenThrow(new IllegalArgumentException("signature verification failed"));

        assertThatThrownBy(() -> service.handlePaymentNotification(
                "serial", "timestamp", "nonce", "bad-signature", "raw-body"))
                .isInstanceOf(IllegalArgumentException.class);

        verify(paymentTransactionService, never()).settleWechatPayment(
                any(), any(), any(), any(), any());
    }

    @Test
    void nullOrMalformedPaymentNotificationIsRejectedAsBadRequest() {
        when(gateway.parsePaymentNotification(any(RequestParam.class))).thenReturn(null);
        assertBadPaymentNotification();

        Transaction malformed = validPayment();
        malformed.setSuccessTime("invalid-time");
        when(gateway.parsePaymentNotification(any(RequestParam.class))).thenReturn(malformed);
        assertBadPaymentNotification();

        verify(paymentTransactionService, never()).settleWechatPayment(
                any(), any(), any(), any(), any());
    }

    @Test
    void successfulRefundNotificationPassesValidatedAmountsToRefundService() {
        when(gateway.parseRefundNotification(any(RequestParam.class))).thenReturn(validRefund());

        service.handleRefundNotification("serial", "timestamp", "nonce", "signature", "raw-body");

        verify(refundService).completeRefund(REFUND_NO, PAYMENT_NO, "wx-transaction-001",
                new BigDecimal("123.45"), new BigDecimal("123.45"), "wx-refund-001");
    }

    @Test
    void processingRefundNotificationDoesNotCompleteOrFailRefund() {
        RefundNotification notification = new RefundNotification();
        notification.setRefundStatus(Status.PROCESSING);
        when(gateway.parseRefundNotification(any(RequestParam.class))).thenReturn(notification);

        service.handleRefundNotification("serial", "timestamp", "nonce", "signature", "raw-body");

        verify(refundService, never()).completeRefund(any(), any(), any(), any(), any(), any());
        verify(refundService, never()).failRefund(any(), any());
    }

    @Test
    void closedRefundNotificationMarksOnlyThatRefundFailed() {
        RefundNotification notification = new RefundNotification();
        notification.setRefundStatus(Status.CLOSED);
        notification.setOutRefundNo(REFUND_NO);
        when(gateway.parseRefundNotification(any(RequestParam.class))).thenReturn(notification);

        service.handleRefundNotification("serial", "timestamp", "nonce", "signature", "raw-body");

        verify(refundService).failRefund(REFUND_NO, "微信退款状态：CLOSED");
        verify(refundService, never()).completeRefund(any(), any(), any(), any(), any(), any());
    }

    @Test
    void malformedSuccessfulRefundNotificationIsRejected() {
        RefundNotification notification = new RefundNotification();
        notification.setRefundStatus(Status.SUCCESS);
        notification.setOutRefundNo(REFUND_NO);
        notification.setAmount(new Amount());
        when(gateway.parseRefundNotification(any(RequestParam.class))).thenReturn(notification);

        assertThatThrownBy(() -> service.handleRefundNotification(
                "serial", "timestamp", "nonce", "signature", "raw-body"))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(refundService, never()).completeRefund(any(), any(), any(), any(), any(), any());
    }

    @Test
    void nullRefundNotificationAndMissingTerminalRefundNumberAreRejected() {
        when(gateway.parseRefundNotification(any(RequestParam.class))).thenReturn(null);
        assertBadRefundNotification();

        RefundNotification closed = new RefundNotification();
        closed.setRefundStatus(Status.CLOSED);
        when(gateway.parseRefundNotification(any(RequestParam.class))).thenReturn(closed);
        assertBadRefundNotification();

        verify(refundService, never()).failRefund(any(), any());
        verify(refundService, never()).completeRefund(any(), any(), any(), any(), any(), any());
    }

    @Test
    void successfulRefundRequiresMerchantRefundNumber() {
        RefundNotification notification = validRefund();
        notification.setOutRefundNo(null);
        when(gateway.parseRefundNotification(any(RequestParam.class))).thenReturn(notification);

        assertBadRefundNotification();

        verify(refundService, never()).completeRefund(any(), any(), any(), any(), any(), any());
    }

    @Test
    void successfulRefundRejectsNonCnyAndMissingWechatRefundId() {
        RefundNotification nonCny = validRefund();
        nonCny.getAmount().setCurrency("USD");
        when(gateway.parseRefundNotification(any(RequestParam.class))).thenReturn(nonCny);
        assertBadRefundNotification();

        RefundNotification missingProviderRefundId = validRefund();
        missingProviderRefundId.setRefundId(null);
        when(gateway.parseRefundNotification(any(RequestParam.class))).thenReturn(missingProviderRefundId);
        assertBadRefundNotification();

        verify(refundService, never()).completeRefund(any(), any(), any(), any(), any(), any());
    }

    private void assertBadPaymentNotification() {
        assertThatThrownBy(() -> service.handlePaymentNotification(
                "serial", "timestamp", "nonce", "signature", "raw-body"))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private void assertBadRefundNotification() {
        assertThatThrownBy(() -> service.handleRefundNotification(
                "serial", "timestamp", "nonce", "signature", "raw-body"))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private Transaction validPayment() {
        Transaction transaction = new Transaction();
        transaction.setAppid(APP_ID);
        transaction.setMchid(MERCHANT_ID);
        transaction.setOutTradeNo(PAYMENT_NO);
        transaction.setTransactionId("wx-transaction-001");
        transaction.setTradeState(Transaction.TradeStateEnum.SUCCESS);
        transaction.setTradeType(Transaction.TradeTypeEnum.JSAPI);
        transaction.setSuccessTime("2026-11-20T05:12:13+00:00");
        TransactionAmount amount = new TransactionAmount();
        amount.setTotal(12345);
        amount.setCurrency("CNY");
        transaction.setAmount(amount);
        TransactionPayer payer = new TransactionPayer();
        payer.setOpenid("wx-openid-001");
        transaction.setPayer(payer);
        return transaction;
    }

    private RefundNotification validRefund() {
        RefundNotification notification = new RefundNotification();
        notification.setOutRefundNo(REFUND_NO);
        notification.setRefundId("wx-refund-001");
        notification.setOutTradeNo(PAYMENT_NO);
        notification.setTransactionId("wx-transaction-001");
        notification.setRefundStatus(Status.SUCCESS);
        Amount amount = new Amount();
        amount.setRefund(12345L);
        amount.setTotal(12345L);
        amount.setCurrency("CNY");
        notification.setAmount(amount);
        return notification;
    }

    private PaymentEntity pendingWechatPayment() {
        PaymentEntity payment = new PaymentEntity();
        payment.setId(33L);
        payment.setBookingId(44L);
        payment.setPaymentNo(PAYMENT_NO);
        payment.setPaymentMethod(PaymentMethod.WECHAT);
        payment.setAmount(new BigDecimal("123.45"));
        payment.setStatus(PaymentStatus.PENDING);
        return payment;
    }

    private BookingLockEntity pendingBooking(Long ownerId) {
        BookingLockEntity booking = new BookingLockEntity();
        booking.setId(44L);
        booking.setMemberId(ownerId);
        booking.setStatus(BookingLockStatus.PENDING);
        booking.setExpiresAt(LocalDateTime.now(BUSINESS_ZONE).plusMinutes(5));
        return booking;
    }

    private PrepayWithRequestPaymentResponse validPrepayResponse() {
        PrepayWithRequestPaymentResponse response = new PrepayWithRequestPaymentResponse();
        response.setAppId(APP_ID);
        response.setTimeStamp("1763620000");
        response.setNonceStr("nonce");
        response.setPackageVal("prepay_id=prepay-001");
        response.setSignType("RSA");
        response.setPaySign("signature");
        return response;
    }
}
