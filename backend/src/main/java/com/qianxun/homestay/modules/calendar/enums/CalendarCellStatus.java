package com.qianxun.homestay.modules.calendar.enums;

import com.qianxun.homestay.modules.order.enums.OrderStatus;

/** 后台房态日历使用的单元格状态编码。 */
public enum CalendarCellStatus {
    /** 当前住宿夜晚空闲。 */
    AVAILABLE,
    /** 当前住宿夜晚由管理员封房。 */
    BLOCKED,
    /** 订单入住日显示的待确认状态。 */
    PENDING_CONFIRM,
    /** 订单入住日显示的待入住状态。 */
    PENDING_CHECKIN,
    /** 订单入住日显示的已入住状态。 */
    CHECKED_IN,
    /** 订单入住日显示的已退房状态。 */
    COMPLETED,
    /** 订单入住日之后的连续住宿夜晚标记，不是订单状态。 */
    OCCUPIED;

    /** 将正式订单状态映射为入住日格子状态。 */
    public static CalendarCellStatus fromOrderStatus(OrderStatus status) {
        return switch (status) {
            case PENDING_CONFIRM -> PENDING_CONFIRM;
            case PENDING_CHECKIN -> PENDING_CHECKIN;
            case CHECKED_IN -> CHECKED_IN;
            case COMPLETED -> COMPLETED;
            case CANCELLED -> AVAILABLE;
        };
    }
}
