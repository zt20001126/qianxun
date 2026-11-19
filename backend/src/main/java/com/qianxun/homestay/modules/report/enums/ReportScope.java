package com.qianxun.homestay.modules.report.enums;

import com.qianxun.homestay.modules.order.enums.OrderStatus;

/** 月报订单范围；取消订单不属于任何范围。 */
public enum ReportScope {
    /** 包含所有未取消订单。 */
    ALL,
    /** 仅包含待确认、待入住订单。 */
    NOT_CHECKED_IN,
    /** 仅包含已入住、已完成订单。 */
    CHECKED_IN;

    /** 判断订单状态是否属于当前报表筛选范围。 */
    public boolean includes(OrderStatus status) {
        return switch (this) {
            case ALL -> status != OrderStatus.CANCELLED;
            case NOT_CHECKED_IN -> status == OrderStatus.PENDING_CONFIRM
                    || status == OrderStatus.PENDING_CHECKIN;
            case CHECKED_IN -> status == OrderStatus.CHECKED_IN || status == OrderStatus.COMPLETED;
        };
    }
}
