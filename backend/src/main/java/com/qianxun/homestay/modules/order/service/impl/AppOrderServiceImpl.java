package com.qianxun.homestay.modules.order.service.impl;

import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.common.util.ApiTime;
import com.qianxun.homestay.modules.order.entity.OrderViewEntity;
import com.qianxun.homestay.modules.order.entity.OrderEntity;
import com.qianxun.homestay.modules.order.domain.OrderCancellationDecision;
import com.qianxun.homestay.modules.order.domain.OrderCancellationPolicy;
import com.qianxun.homestay.modules.order.enums.OrderStatus;
import com.qianxun.homestay.modules.order.mapper.OrderMapper;
import com.qianxun.homestay.modules.payment.entity.PaymentEntity;
import com.qianxun.homestay.modules.payment.mapper.PaymentMapper;
import com.qianxun.homestay.modules.order.service.AppOrderService;
import com.qianxun.homestay.modules.order.vo.AppOrderDetailVO;
import com.qianxun.homestay.modules.order.vo.AppOrderListItemVO;
import com.qianxun.homestay.modules.order.vo.AppOrderPageVO;
import com.qianxun.homestay.modules.order.vo.OrderCancellationEligibilityVO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 按会员 ID 限定订单查询并转换为面向顾客的视图。 */
@Service
public class AppOrderServiceImpl implements AppOrderService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final OrderMapper orderMapper;
    private final OrderCancellationPolicy cancellationPolicy;
    private final PaymentMapper paymentMapper;

    public AppOrderServiceImpl(
            OrderMapper orderMapper, OrderCancellationPolicy cancellationPolicy, PaymentMapper paymentMapper) {
        this.orderMapper = orderMapper;
        this.cancellationPolicy = cancellationPolicy;
        this.paymentMapper = paymentMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public AppOrderPageVO findMyOrders(Long memberId, OrderStatus status, int page, int size) {
        long offset = (long) (page - 1) * size;
        List<AppOrderListItemVO> list = orderMapper.findAppPage(
                memberId, status == null ? null : status.name(), offset, size).stream()
                .map(this::toListItem)
                .toList();
        return new AppOrderPageVO(list, orderMapper.countApp(memberId, status == null ? null : status.name()), page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public AppOrderDetailVO findMyOrder(Long memberId, Long orderId) {
        OrderViewEntity order = orderMapper.findAppViewById(memberId, orderId);
        if (order == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "订单不存在");
        }
        PaymentEntity payment = paymentMapper.findByOrderId(orderId);
        return new AppOrderDetailVO(
                order.getId(), order.getOrderNo(), order.getRoomId(), order.getRoomNumber(), order.getGuestName(),
                order.getPhone(), order.getGuestCount(), order.getCheckIn(), order.getCheckOut(), order.getNights(),
                order.getStatus(), order.getRemark(), money(order.getPaidAmount()), money(order.getRoomAmount()),
                order.getPointsUsed(), money(order.getDiscountAmount()),
                payment == null ? null : payment.getPaymentMethod(), ApiTime.fromShanghai(order.getCreatedAt()),
                ApiTime.fromShanghai(order.getUpdatedAt()));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderCancellationEligibilityVO getCancellationEligibility(Long memberId, Long orderId) {
        OrderEntity order = orderMapper.findAppById(memberId, orderId);
        if (order == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "订单不存在");
        }
        OrderCancellationDecision decision = cancellationPolicy.evaluate(order, LocalDateTime.now(BUSINESS_ZONE));
        return new OrderCancellationEligibilityVO(
                decision.eligible(), decision.reason(), ApiTime.fromShanghai(decision.deadline()));
    }

    private AppOrderListItemVO toListItem(OrderViewEntity order) {
        return new AppOrderListItemVO(
                order.getId(), order.getOrderNo(), order.getRoomId(), order.getRoomNumber(), order.getChannelId(),
                order.getCreatorId(), order.getGuestName(), order.getPhone(), order.getCheckIn(), order.getCheckOut(),
                order.getNights(), order.getStatus(), money(order));
    }

    private String money(OrderViewEntity order) {
        return money(order.getPaidAmount());
    }

    private String money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.UNNECESSARY).toPlainString();
    }
}
