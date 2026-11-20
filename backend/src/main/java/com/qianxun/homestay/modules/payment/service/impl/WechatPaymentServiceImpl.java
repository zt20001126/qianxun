package com.qianxun.homestay.modules.payment.service.impl;

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
import com.qianxun.homestay.modules.payment.service.WechatPaymentService;
import com.qianxun.homestay.modules.payment.vo.WechatPrepayVO;
import com.qianxun.homestay.modules.refund.service.WechatRefundService;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayWithRequestPaymentResponse;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.refund.model.RefundNotification;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** 编排微信预支付、支付回调和退款回调。 */
@Service
public class WechatPaymentServiceImpl implements WechatPaymentService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final BookingLockMapper bookingLockMapper;
    private final PaymentMapper paymentMapper;
    private final PaymentTransactionService paymentTransactionService;
    private final WechatIdentityService wechatIdentityService;
    private final WechatPayGateway wechatPayGateway;
    private final WechatPayProperties properties;
    private final WechatRefundService wechatRefundService;

    public WechatPaymentServiceImpl(
            BookingLockMapper bookingLockMapper,
            PaymentMapper paymentMapper,
            PaymentTransactionService paymentTransactionService,
            WechatIdentityService wechatIdentityService,
            WechatPayGateway wechatPayGateway,
            WechatPayProperties properties,
            WechatRefundService wechatRefundService) {
        this.bookingLockMapper = bookingLockMapper;
        this.paymentMapper = paymentMapper;
        this.paymentTransactionService = paymentTransactionService;
        this.wechatIdentityService = wechatIdentityService;
        this.wechatPayGateway = wechatPayGateway;
        this.properties = properties;
        this.wechatRefundService = wechatRefundService;
    }

    @Override
    public WechatPrepayVO createPrepay(Long memberId, String paymentNo, String loginCode, String phoneCode) {
        requireEnabled();
        PaymentEntity payment = paymentMapper.findByPaymentNo(paymentNo);
        if (payment == null || payment.getPaymentMethod() != PaymentMethod.WECHAT) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "微信支付交易不存在");
        }
        BookingLockEntity booking = bookingLockMapper.findById(payment.getBookingId());
        if (booking == null || !memberId.equals(booking.getMemberId())) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "微信支付交易不存在");
        }
        if (payment.getStatus() != PaymentStatus.PENDING || booking.getStatus() != BookingLockStatus.PENDING
                || !booking.getExpiresAt().isAfter(LocalDateTime.now(BUSINESS_ZONE))) {
            throw new BusinessException(HttpStatus.CONFLICT, "支付锁房已关闭或过期，请重新预订");
        }

        String openid = wechatIdentityService.verifyAndBindOpenId(memberId, loginCode, phoneCode);
        PrepayWithRequestPaymentResponse response = wechatPayGateway.createPrepay(
                paymentNo, payment.getAmount(), openid, booking.getExpiresAt());
        String packageValue = response.getPackageVal();
        if (packageValue == null || !packageValue.startsWith("prepay_id=") || packageValue.length() <= 10) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "微信预支付响应无效，请重试");
        }
        if (paymentMapper.updateWechatPrepayId(payment.getId(), packageValue.substring(10)) != 1) {
            throw new BusinessException(HttpStatus.CONFLICT, "支付状态已变化，请查询订单");
        }
        return new WechatPrepayVO(
                response.getAppId(), response.getTimeStamp(), response.getNonceStr(), response.getPackageVal(),
                response.getSignType(), response.getPaySign());
    }

    @Override
    public void handlePaymentNotification(
            String serial, String timestamp, String nonce, String signature, String body) {
        Transaction transaction = wechatPayGateway.parsePaymentNotification(
                requestParam(serial, timestamp, nonce, signature, body));
        if (transaction == null
                || !properties.getAppId().equals(transaction.getAppid())
                || !properties.getMerchantId().equals(transaction.getMchid())
                || transaction.getTradeState() == null
                || !"SUCCESS".equals(transaction.getTradeState().name())
                || transaction.getAmount() == null
                || transaction.getAmount().getTotal() == null
                || !"CNY".equals(transaction.getAmount().getCurrency())
                || transaction.getTradeType() == null || !"JSAPI".equals(transaction.getTradeType().name())
                || transaction.getSuccessTime() == null
                || transaction.getTransactionId() == null || transaction.getOutTradeNo() == null
                || transaction.getPayer() == null || transaction.getPayer().getOpenid() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "微信支付通知内容无效");
        }
        BigDecimal amount = BigDecimal.valueOf(transaction.getAmount().getTotal(), 2);
        LocalDateTime paidAt;
        try {
            paidAt = OffsetDateTime.parse(transaction.getSuccessTime()).atZoneSameInstant(BUSINESS_ZONE)
                    .toLocalDateTime();
        } catch (DateTimeParseException exception) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "微信支付通知成功时间格式无效");
        }
        paymentTransactionService.settleWechatPayment(
                transaction.getOutTradeNo(), transaction.getTransactionId(), amount, paidAt,
                transaction.getPayer().getOpenid());
    }

    @Override
    public void handleRefundNotification(
            String serial, String timestamp, String nonce, String signature, String body) {
        RefundNotification notification = wechatPayGateway.parseRefundNotification(
                requestParam(serial, timestamp, nonce, signature, body));
        if (notification == null || notification.getRefundStatus() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "微信退款通知状态无效");
        }
        String refundStatus = notification.getRefundStatus().name();
        if ("CLOSED".equals(refundStatus) || "ABNORMAL".equals(refundStatus)) {
            if (notification.getOutRefundNo() == null || notification.getOutRefundNo().isBlank()) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "微信退款通知缺少退款单号");
            }
            wechatRefundService.failRefund(notification.getOutRefundNo(), "微信退款状态：" + refundStatus);
            return;
        }
        if (!"SUCCESS".equals(refundStatus)) {
            return;
        }
        if (notification.getOutRefundNo() == null || notification.getOutRefundNo().isBlank()
                || notification.getAmount() == null || notification.getAmount().getRefund() == null
                || notification.getAmount().getTotal() == null || notification.getRefundId() == null
                || !"CNY".equals(notification.getAmount().getCurrency())
                || notification.getTransactionId() == null || notification.getOutTradeNo() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "微信退款通知金额无效");
        }
        wechatRefundService.completeRefund(
                notification.getOutRefundNo(), notification.getOutTradeNo(), notification.getTransactionId(),
                BigDecimal.valueOf(notification.getAmount().getRefund(), 2),
                BigDecimal.valueOf(notification.getAmount().getTotal(), 2), notification.getRefundId());
    }

    private RequestParam requestParam(String serial, String timestamp, String nonce, String signature, String body) {
        return new RequestParam.Builder().serialNumber(serial).timestamp(timestamp).nonce(nonce)
                .signature(signature).body(body).build();
    }

    private void requireEnabled() {
        if (!properties.isEnabled()) {
            throw new BusinessException(HttpStatus.CONFLICT, "微信支付当前未启用");
        }
    }
}
