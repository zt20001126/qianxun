package com.qianxun.homestay.modules.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.qianxun.homestay.modules.order.domain.OrderCancellationDecision;
import com.qianxun.homestay.modules.order.domain.OrderCancellationPolicy;
import com.qianxun.homestay.modules.order.entity.OrderEntity;
import com.qianxun.homestay.modules.order.enums.OrderStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/** 覆盖自助退款状态及入住日 18:00 北京时间边界。 */
class OrderCancellationPolicyTest {

    private final OrderCancellationPolicy policy = new OrderCancellationPolicy();

    @Test
    void deadlineIsExclusiveAtExactlyEighteenHundred() {
        OrderEntity order = cancellableOrder();
        LocalDateTime deadline = order.getCheckIn().atTime(18, 0);

        OrderCancellationDecision before = policy.evaluate(order, deadline.minusSeconds(1));
        OrderCancellationDecision at = policy.evaluate(order, deadline);

        assertThat(before.eligible()).isTrue();
        assertThat(before.deadline()).isEqualTo(deadline);
        assertThat(at.eligible()).isFalse();
    }

    @Test
    void checkedInAndCompletedOrdersAreNotEligibleBeforeDeadline() {
        OrderEntity order = cancellableOrder();
        order.setStatus(OrderStatus.CHECKED_IN);

        assertThat(policy.evaluate(order, order.getCheckIn().atTime(10, 0)).eligible()).isFalse();

        order.setStatus(OrderStatus.COMPLETED);
        assertThat(policy.evaluate(order, order.getCheckIn().atTime(10, 0)).eligible()).isFalse();
    }

    private OrderEntity cancellableOrder() {
        OrderEntity order = new OrderEntity();
        order.setCheckIn(LocalDate.of(2026, 11, 20));
        order.setStatus(OrderStatus.PENDING_CONFIRM);
        return order;
    }
}
