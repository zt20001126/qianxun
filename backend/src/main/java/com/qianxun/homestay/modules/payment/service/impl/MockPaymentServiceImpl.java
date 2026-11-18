package com.qianxun.homestay.modules.payment.service.impl;

import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.modules.booking.entity.BookingLockEntity;
import com.qianxun.homestay.modules.booking.mapper.BookingLockMapper;
import com.qianxun.homestay.modules.order.entity.OrderEntity;
import com.qianxun.homestay.modules.order.mapper.OrderMapper;
import com.qianxun.homestay.modules.payment.domain.PaymentAttemptResult;
import com.qianxun.homestay.modules.payment.entity.PaymentEntity;
import com.qianxun.homestay.modules.payment.enums.PaymentStatus;
import com.qianxun.homestay.modules.payment.mapper.PaymentMapper;
import com.qianxun.homestay.modules.payment.service.MockPaymentService;
import com.qianxun.homestay.modules.payment.service.PaymentTimeoutService;
import com.qianxun.homestay.modules.payment.service.PaymentTransactionService;
import com.qianxun.homestay.modules.payment.vo.MockPayVO;
import com.qianxun.homestay.modules.payment.vo.PaymentStatusVO;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** 编排支付查询与独立事务命令，确保超时关闭先提交再返回冲突。 */
@Service
public class MockPaymentServiceImpl implements MockPaymentService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final BookingLockMapper bookingLockMapper;
    private final OrderMapper orderMapper;
    private final PaymentMapper paymentMapper;
    private final PaymentTimeoutService paymentTimeoutService;
    private final PaymentTransactionService paymentTransactionService;

    public MockPaymentServiceImpl(
            BookingLockMapper bookingLockMapper,
            OrderMapper orderMapper,
            PaymentMapper paymentMapper,
            PaymentTimeoutService paymentTimeoutService,
            PaymentTransactionService paymentTransactionService) {
        this.bookingLockMapper = bookingLockMapper;
        this.orderMapper = orderMapper;
        this.paymentMapper = paymentMapper;
        this.paymentTimeoutService = paymentTimeoutService;
        this.paymentTransactionService = paymentTransactionService;
    }

    @Override
    public PaymentStatusVO findStatus(Long memberId, String paymentNo) {
        PaymentEntity payment = requirePayment(paymentNo);
        BookingLockEntity booking = requireOwnedBooking(payment, memberId);
        if (payment.getStatus() == PaymentStatus.PENDING
                && !booking.getExpiresAt().isAfter(now())) {
            paymentTimeoutService.closeExpiredPayment(payment.getId());
            payment = requirePayment(paymentNo);
        }
        String orderNo = orderNoIfOwnedPaymentSucceeded(payment, memberId);
        return new PaymentStatusVO(payment.getPaymentNo(), payment.getStatus(), payment.getOrderId(), orderNo);
    }

    @Override
    public MockPayVO mockPay(Long memberId, String paymentNo) {
        PaymentAttemptResult result = paymentTransactionService.pay(memberId, paymentNo);
        if (result.expired()) {
            throw new BusinessException(HttpStatus.CONFLICT, "支付交易已过期，请重新预订");
        }
        return result.payment();
    }

    private PaymentEntity requirePayment(String paymentNo) {
        PaymentEntity payment = paymentMapper.findByPaymentNo(paymentNo);
        if (payment == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "支付交易不存在");
        }
        return payment;
    }

    private BookingLockEntity requireOwnedBooking(PaymentEntity payment, Long memberId) {
        BookingLockEntity booking = bookingLockMapper.findById(payment.getBookingId());
        if (booking == null || !memberId.equals(booking.getMemberId())) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "支付交易不存在");
        }
        return booking;
    }

    private String orderNoIfOwnedPaymentSucceeded(PaymentEntity payment, Long memberId) {
        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            return null;
        }
        if (payment.getOrderId() == null) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "成功支付未关联正式订单");
        }
        OrderEntity order = orderMapper.findById(payment.getOrderId());
        if (order == null || !memberId.equals(order.getMemberId())) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "支付关联订单不存在");
        }
        return order.getOrderNo();
    }

    private LocalDateTime now() {
        return LocalDateTime.now(BUSINESS_ZONE);
    }
}
