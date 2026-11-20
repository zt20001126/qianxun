package com.qianxun.homestay.modules.payment.service.impl;

import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.modules.availability.service.AvailabilityService;
import com.qianxun.homestay.modules.booking.entity.BookingLockEntity;
import com.qianxun.homestay.modules.booking.enums.BookingLockStatus;
import com.qianxun.homestay.modules.booking.mapper.BookingLockMapper;
import com.qianxun.homestay.modules.member.mapper.MemberMapper;
import com.qianxun.homestay.modules.payment.entity.PaymentEntity;
import com.qianxun.homestay.modules.payment.enums.PaymentStatus;
import com.qianxun.homestay.modules.payment.enums.PaymentMethod;
import com.qianxun.homestay.modules.payment.mapper.PaymentMapper;
import com.qianxun.homestay.modules.payment.provider.WechatPayGateway;
import com.qianxun.homestay.modules.payment.service.PaymentTransactionService;
import com.qianxun.homestay.modules.payment.service.PaymentTimeoutService;
import com.wechat.pay.java.service.payments.model.Transaction;
import java.time.OffsetDateTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** 在独立事务内关闭过期交易并释放临时锁房及积分预占。 */
@Service
public class PaymentTimeoutServiceImpl implements PaymentTimeoutService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final AvailabilityService availabilityService;
    private final BookingLockMapper bookingLockMapper;
    private final MemberMapper memberMapper;
    private final PaymentMapper paymentMapper;
    private final WechatPayGateway wechatPayGateway;
    private final PaymentTransactionService paymentTransactionService;
    private final TransactionTemplate transactionTemplate;

    public PaymentTimeoutServiceImpl(
            AvailabilityService availabilityService,
            BookingLockMapper bookingLockMapper,
            MemberMapper memberMapper,
            PaymentMapper paymentMapper,
            WechatPayGateway wechatPayGateway,
            PaymentTransactionService paymentTransactionService,
            PlatformTransactionManager transactionManager) {
        this.availabilityService = availabilityService;
        this.bookingLockMapper = bookingLockMapper;
        this.memberMapper = memberMapper;
        this.paymentMapper = paymentMapper;
        this.wechatPayGateway = wechatPayGateway;
        this.paymentTransactionService = paymentTransactionService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void closeExpiredPayment(Long paymentId) {
        PaymentEntity observed = paymentMapper.findById(paymentId);
        if (observed == null || observed.getStatus() != PaymentStatus.PENDING) {
            return;
        }
        BookingLockEntity observedBooking = bookingLockMapper.findById(observed.getBookingId());
        if (observedBooking == null || observedBooking.getExpiresAt().isAfter(now())) {
            return;
        }
        if (observed.getPaymentMethod() == PaymentMethod.WECHAT
                && observed.getWechatPrepayId() != null && !closeWechatPayment(observed)) {
            return;
        }
        transactionTemplate.executeWithoutResult(status -> closeExpiredPaymentLocally(paymentId));
    }

    private void closeExpiredPaymentLocally(Long paymentId) {
        PaymentEntity payment = paymentMapper.findByIdForUpdate(paymentId);
        if (payment == null || payment.getStatus() != PaymentStatus.PENDING) {
            return;
        }
        BookingLockEntity booking = bookingLockMapper.findByIdForUpdate(payment.getBookingId());
        if (booking == null) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "支付交易关联的临时预订不存在");
        }
        if (booking.getStatus() != BookingLockStatus.PENDING) {
            // 已成功或已关闭的锁房不能被超时扫描再次释放。
            if (booking.getStatus() == BookingLockStatus.PAID) {
                throw new BusinessException(HttpStatus.CONFLICT, "支付与锁房状态不一致");
            }
            paymentMapper.markClosed(payment.getId());
            return;
        }
        if (booking.getExpiresAt().isAfter(now())) {
            return;
        }

        availabilityService.lockRoomsForUpdate(List.of(booking.getRoomId()));
        if (memberMapper.findByIdForUpdate(booking.getMemberId()) == null) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "支付交易关联会员不存在");
        }

        // P08 和超时扫描先争用 payment 行锁，只有先拿到锁且检查未过期的一方能提交状态转换。
        if (paymentMapper.markClosed(payment.getId()) != 1) {
            return;
        }
        if (bookingLockMapper.updateStatus(booking.getId(), BookingLockStatus.EXPIRED.name()) != 1) {
            throw new BusinessException(HttpStatus.CONFLICT, "临时预订状态已变化，请重试");
        }
    }

    /** 微信未确认关闭时保留锁房；若已支付，则先按微信通知路径结算。 */
    private boolean closeWechatPayment(PaymentEntity payment) {
        try {
            wechatPayGateway.closePayment(payment.getPaymentNo());
            return true;
        } catch (RuntimeException closeException) {
            try {
                Transaction transaction = wechatPayGateway.queryPayment(payment.getPaymentNo());
                String state = transaction.getTradeState() == null ? "" : transaction.getTradeState().name();
                if ("SUCCESS".equals(state)) {
                    paymentTransactionService.settleWechatPayment(
                            transaction.getOutTradeNo(), transaction.getTransactionId(),
                            java.math.BigDecimal.valueOf(transaction.getAmount().getTotal(), 2),
                            OffsetDateTime.parse(transaction.getSuccessTime()).atZoneSameInstant(BUSINESS_ZONE)
                                    .toLocalDateTime(),
                            transaction.getPayer() == null ? null : transaction.getPayer().getOpenid());
                    return false;
                }
                return "CLOSED".equals(state);
            } catch (RuntimeException queryException) {
                // 微信侧状态不明时绝不释放库存或积分预占，等待后续扫描再次对账。
                return false;
            }
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now(BUSINESS_ZONE);
    }
}
