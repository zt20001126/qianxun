package com.qianxun.homestay.modules.refund.service.impl;

import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.modules.order.domain.OrderCancellationDecision;
import com.qianxun.homestay.modules.order.domain.OrderCancellationPolicy;
import com.qianxun.homestay.modules.order.entity.OrderEntity;
import com.qianxun.homestay.modules.order.enums.OrderStatus;
import com.qianxun.homestay.modules.order.mapper.OrderMapper;
import com.qianxun.homestay.modules.payment.entity.PaymentEntity;
import com.qianxun.homestay.modules.payment.enums.PaymentMethod;
import com.qianxun.homestay.modules.payment.enums.PaymentStatus;
import com.qianxun.homestay.modules.payment.mapper.PaymentMapper;
import com.qianxun.homestay.modules.points.service.PointsBalanceService;
import com.qianxun.homestay.modules.refund.domain.WechatRefundCommand;
import com.qianxun.homestay.modules.refund.entity.RefundEntity;
import com.qianxun.homestay.modules.refund.enums.RefundStatus;
import com.qianxun.homestay.modules.refund.mapper.RefundMapper;
import com.qianxun.homestay.modules.refund.service.WechatRefundTransactionService;
import com.qianxun.homestay.modules.refund.vo.RefundResultVO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

/** 事务化准备微信退款，并在微信确认后取消订单和返还积分。 */
@Service
public class WechatRefundTransactionServiceImpl implements WechatRefundTransactionService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private final OrderCancellationPolicy cancellationPolicy;
    private final OrderMapper orderMapper;
    private final PaymentMapper paymentMapper;
    private final RefundMapper refundMapper;
    private final PointsBalanceService pointsBalanceService;
    private final ApplicationEventPublisher eventPublisher;

    public WechatRefundTransactionServiceImpl(
            OrderCancellationPolicy cancellationPolicy, OrderMapper orderMapper,
            PaymentMapper paymentMapper, RefundMapper refundMapper, PointsBalanceService pointsBalanceService, ApplicationEventPublisher eventPublisher) {
        this.cancellationPolicy = cancellationPolicy;
        this.orderMapper = orderMapper;
        this.paymentMapper = paymentMapper;
        this.refundMapper = refundMapper;
        this.pointsBalanceService = pointsBalanceService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public WechatRefundCommand prepare(Long memberId, Long orderId, String reason) {
        OrderEntity order = orderMapper.findAppByIdForUpdate(memberId, orderId);
        if (order == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "订单不存在");
        }
        RefundEntity latest = refundMapper.findLatestByOrderId(orderId);
        if (order.getStatus() == OrderStatus.CANCELLED) {
            if (latest != null && latest.getStatus() == RefundStatus.SUCCESS) {
                return command(latest, paymentMapper.findByOrderIdForUpdate(orderId));
            }
            throw new BusinessException(HttpStatus.CONFLICT, "订单已取消，未找到成功退款记录");
        }
        if (latest != null && latest.getStatus() == RefundStatus.PENDING) {
            return command(latest, paymentMapper.findByOrderIdForUpdate(orderId));
        }
        OrderCancellationDecision decision = cancellationPolicy.evaluate(order, LocalDateTime.now(BUSINESS_ZONE));
        if (!decision.eligible()) {
            throw new BusinessException(HttpStatus.CONFLICT, decision.reason());
        }
        PaymentEntity payment = paymentMapper.findByOrderIdForUpdate(orderId);
        if (payment == null || payment.getStatus() != PaymentStatus.SUCCESS || !orderId.equals(payment.getOrderId())
                || payment.getPaymentMethod() != PaymentMethod.WECHAT) {
            throw new BusinessException(HttpStatus.CONFLICT, "订单没有可退款的微信支付记录");
        }
        if (refundMapper.findSuccessfulByOrderId(orderId) != null) {
            throw new BusinessException(HttpStatus.CONFLICT, "订单已存在成功退款记录");
        }

        RefundEntity refund = new RefundEntity();
        refund.setOrderId(orderId);
        refund.setPaymentId(payment.getId());
        refund.setRefundNo("RF" + UUID.randomUUID().toString().replace("-", "").toUpperCase());
        refund.setAmount(payment.getAmount());
        refund.setStatus(RefundStatus.PENDING);
        refund.setReason(normalizeReason(reason));
        if (refundMapper.insert(refund) != 1) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "微信退款流水创建失败");
        }
        return command(refund, payment);
    }

    @Override
    @Transactional
    public RefundResultVO complete(
            String refundNo, String paymentNo, String transactionId,
            BigDecimal refundAmount, BigDecimal paymentAmount,
            String wechatRefundId) {
        RefundEntity refund = refundMapper.findByRefundNoForUpdate(refundNo);
        if (refund == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "微信退款流水不存在");
        }
        OrderEntity order = orderMapper.findByIdForUpdate(refund.getOrderId());
        PaymentEntity payment = paymentMapper.findByOrderIdForUpdate(refund.getOrderId());
        if (order == null || payment == null || !payment.getPaymentNo().equals(paymentNo)
                || !java.util.Objects.equals(payment.getTransactionId(), transactionId)
                || refund.getAmount().compareTo(refundAmount) != 0 || payment.getAmount().compareTo(paymentAmount) != 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "微信退款通知与本地订单金额不一致");
        }
        if (refund.getStatus() == RefundStatus.SUCCESS) {
            return new RefundResultVO(refund.getId(), refund.getRefundNo(), refund.getStatus());
        }
        if (refund.getStatus() != RefundStatus.PENDING) {
            throw new BusinessException(HttpStatus.CONFLICT, "退款流水已关闭");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException(HttpStatus.CONFLICT, "退款成功通知与订单状态不一致");
        }
        if (refundMapper.markWechatSuccess(refund.getId(), wechatRefundId, LocalDateTime.now(BUSINESS_ZONE)) != 1) {
            throw new BusinessException(HttpStatus.CONFLICT, "退款流水状态已变化");
        }
        if (orderMapper.cancelForRefund(order.getId()) != 1) {
            throw new BusinessException(HttpStatus.CONFLICT, "订单状态已变化，退款未完成");
        }
        pointsBalanceService.returnForRefund(order.getMemberId(), order.getId());
        eventPublisher.publishEvent(new com.qianxun.homestay.modules.notification.event.OrderNotificationEvent(
                com.qianxun.homestay.modules.notification.event.OrderNotificationEvent.Type.STAY_CANCELLED, order.getId(), refund.getAmount(), refund.getReason(), LocalDateTime.now(BUSINESS_ZONE)));
        return new RefundResultVO(refund.getId(), refund.getRefundNo(), RefundStatus.SUCCESS);
    }

    @Override
    @Transactional
    public void fail(String refundNo, String reason) {
        RefundEntity refund = refundMapper.findByRefundNoForUpdate(refundNo);
        if (refund != null && refund.getStatus() == RefundStatus.PENDING) {
            refundMapper.markFailed(refund.getId(), reason);
        }
    }

    private WechatRefundCommand command(RefundEntity refund, PaymentEntity payment) {
        if (payment == null || !refund.getPaymentId().equals(payment.getId())) {
            throw new BusinessException(HttpStatus.CONFLICT, "退款流水关联的支付记录不存在");
        }
        return new WechatRefundCommand(refund.getId(), refund.getRefundNo(), payment.getPaymentNo(),
                refund.getAmount(), payment.getAmount(), refund.getReason(), refund.getWechatRefundId(),
                refund.getStatus());
    }

    private String normalizeReason(String reason) {
        return reason == null || reason.isBlank() ? null : reason.trim();
    }
}
