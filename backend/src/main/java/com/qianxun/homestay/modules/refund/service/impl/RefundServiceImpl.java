package com.qianxun.homestay.modules.refund.service.impl;

import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.common.util.ApiTime;
import com.qianxun.homestay.modules.order.domain.OrderCancellationDecision;
import com.qianxun.homestay.modules.order.domain.OrderCancellationPolicy;
import com.qianxun.homestay.modules.order.entity.OrderEntity;
import com.qianxun.homestay.modules.order.enums.OrderStatus;
import com.qianxun.homestay.modules.order.mapper.OrderMapper;
import com.qianxun.homestay.modules.payment.entity.PaymentEntity;
import com.qianxun.homestay.modules.payment.enums.PaymentStatus;
import com.qianxun.homestay.modules.payment.mapper.PaymentMapper;
import com.qianxun.homestay.modules.points.service.PointsBalanceService;
import com.qianxun.homestay.modules.refund.entity.RefundEntity;
import com.qianxun.homestay.modules.refund.enums.RefundStatus;
import com.qianxun.homestay.modules.refund.mapper.RefundMapper;
import com.qianxun.homestay.modules.refund.service.RefundService;
import com.qianxun.homestay.modules.refund.vo.RefundListItemVO;
import com.qianxun.homestay.modules.refund.vo.RefundPageVO;
import com.qianxun.homestay.modules.refund.vo.RefundResultVO;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 编排同步模拟退款，保证退款流水、订单状态与积分返还原子提交。 */
@Service
public class RefundServiceImpl implements RefundService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final OrderCancellationPolicy cancellationPolicy;
    private final OrderMapper orderMapper;
    private final PaymentMapper paymentMapper;
    private final RefundMapper refundMapper;
    private final PointsBalanceService pointsBalanceService;

    public RefundServiceImpl(
            OrderCancellationPolicy cancellationPolicy,
            OrderMapper orderMapper,
            PaymentMapper paymentMapper,
            RefundMapper refundMapper,
            PointsBalanceService pointsBalanceService) {
        this.cancellationPolicy = cancellationPolicy;
        this.orderMapper = orderMapper;
        this.paymentMapper = paymentMapper;
        this.refundMapper = refundMapper;
        this.pointsBalanceService = pointsBalanceService;
    }

    @Override
    @Transactional
    public RefundResultVO refundMyOrder(Long memberId, Long orderId, String reason) {
        OrderEntity order = orderMapper.findAppByIdForUpdate(memberId, orderId);
        if (order == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "订单不存在");
        }

        // 先处理重复请求，确保已经成功的取消在截止时间后重试仍返回原退款结果。
        if (order.getStatus() == OrderStatus.CANCELLED) {
            RefundEntity existing = refundMapper.findSuccessfulByOrderId(orderId);
            if (existing != null) {
                return toResult(existing);
            }
            throw new BusinessException(HttpStatus.CONFLICT, "订单已取消，未找到对应的成功退款记录");
        }

        LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE);
        OrderCancellationDecision decision = cancellationPolicy.evaluate(order, now);
        if (!decision.eligible()) {
            throw new BusinessException(HttpStatus.CONFLICT, decision.reason());
        }

        PaymentEntity payment = paymentMapper.findByOrderIdForUpdate(orderId);
        if (payment == null || payment.getStatus() != PaymentStatus.SUCCESS || !orderId.equals(payment.getOrderId())) {
            throw new BusinessException(HttpStatus.CONFLICT, "订单没有可退款的成功模拟支付记录");
        }
        RefundEntity previousSuccess = refundMapper.findSuccessfulByOrderId(orderId);
        if (previousSuccess != null) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "退款流水与订单状态不一致");
        }

        RefundEntity refund = new RefundEntity();
        refund.setOrderId(orderId);
        refund.setPaymentId(payment.getId());
        refund.setRefundNo("RF" + UUID.randomUUID().toString().replace("-", "").toUpperCase());
        refund.setAmount(payment.getAmount());
        refund.setStatus(RefundStatus.SUCCESS);
        refund.setReason(normalizeReason(reason));
        refund.setRefundedAt(now);
        if (refundMapper.insert(refund) != 1) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "退款流水写入失败");
        }

        if (orderMapper.cancelForRefund(orderId) != 1) {
            throw new BusinessException(HttpStatus.CONFLICT, "订单状态已变化，请重新查询");
        }
        pointsBalanceService.returnForRefund(memberId, orderId);
        return toResult(refund);
    }

    @Override
    @Transactional(readOnly = true)
    public RefundPageVO findAdminRefunds(
            RefundStatus status, LocalDate startDate, LocalDate endDate, int page, int size) {
        validateDateRange(startDate, endDate);
        LocalDateTime startAt = startDate == null ? null : startDate.atStartOfDay();
        LocalDateTime endBefore = endDate == null ? null : endDate.plusDays(1).atStartOfDay();
        long offset = (long) (page - 1) * size;
        List<RefundListItemVO> list = refundMapper.findAdminPage(status, startAt, endBefore, offset, size).stream()
                .map(this::toListItem)
                .toList();
        long total = refundMapper.countAdmin(status, startAt, endBefore);
        return new RefundPageVO(list, total, page, size);
    }

    private RefundResultVO toResult(RefundEntity refund) {
        return new RefundResultVO(refund.getId(), refund.getRefundNo(), refund.getStatus());
    }

    private RefundListItemVO toListItem(RefundEntity refund) {
        String amount = refund.getAmount().setScale(2, RoundingMode.UNNECESSARY).toPlainString();
        return new RefundListItemVO(
                refund.getId(), refund.getRefundNo(), refund.getOrderId(), amount, refund.getStatus(),
                refund.getReason(), ApiTime.fromShanghai(refund.getRefundedAt()));
    }

    private String normalizeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return null;
        }
        return reason.trim();
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "结束日期不能早于开始日期");
        }
    }
}
