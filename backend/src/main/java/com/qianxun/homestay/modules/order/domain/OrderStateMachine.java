package com.qianxun.homestay.modules.order.domain;

import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.modules.order.enums.OrderAction;
import com.qianxun.homestay.modules.order.enums.OrderStatus;
import org.springframework.http.HttpStatus;

/** 集中校验订单状态动作，避免接口各自实现不同的流转规则。 */
public class OrderStateMachine {

    /** 按当前状态和动作计算下一状态；不满足逐级流转时拒绝操作。 */
    public OrderStatus transition(OrderStatus current, OrderAction action) {
        if (action == OrderAction.CONFIRM && current == OrderStatus.PENDING_CONFIRM) {
            return OrderStatus.PENDING_CHECKIN;
        }
        if (action == OrderAction.CHECK_IN && current == OrderStatus.PENDING_CHECKIN) {
            return OrderStatus.CHECKED_IN;
        }
        if (action == OrderAction.CHECK_OUT && current == OrderStatus.CHECKED_IN) {
            return OrderStatus.COMPLETED;
        }
        if (action == OrderAction.CANCEL
                && (current == OrderStatus.PENDING_CONFIRM || current == OrderStatus.PENDING_CHECKIN)) {
            return OrderStatus.CANCELLED;
        }
        throw new BusinessException(HttpStatus.CONFLICT, "当前订单状态不允许执行该操作");
    }
}
