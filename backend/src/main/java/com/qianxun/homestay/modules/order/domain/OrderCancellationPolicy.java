package com.qianxun.homestay.modules.order.domain;

import com.qianxun.homestay.modules.order.entity.OrderEntity;
import com.qianxun.homestay.modules.order.enums.OrderStatus;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;
import org.springframework.stereotype.Component;

/** 集中判断订单自助取消及模拟退款的状态和截止规则。 */
@Component
public class OrderCancellationPolicy {

    private static final Set<OrderStatus> CANCELLABLE_STATUSES = Set.of(
            OrderStatus.PENDING_CONFIRM, OrderStatus.PENDING_CHECKIN);
    private static final LocalTime CANCELLATION_DEADLINE = LocalTime.of(18, 0);

    /** 根据北京时间判断订单是否仍可自助取消。 */
    public OrderCancellationDecision evaluate(OrderEntity order, LocalDateTime now) {
        LocalDateTime deadline = order.getCheckIn().atTime(CANCELLATION_DEADLINE);
        if (!CANCELLABLE_STATUSES.contains(order.getStatus())) {
            return new OrderCancellationDecision(false, "当前订单不支持自助退款，请联系商家", deadline);
        }
        if (!now.isBefore(deadline)) {
            return new OrderCancellationDecision(false, "已超过自助取消时间，请联系商家", deadline);
        }
        return new OrderCancellationDecision(true, "", deadline);
    }
}
