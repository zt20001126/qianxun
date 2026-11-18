package com.qianxun.homestay.modules.order.enums;

/** 统一定义正式订单可持久化的状态编码。 */
public enum OrderStatus {
    /** 订单已创建，等待管理员确认。 */
    PENDING_CONFIRM,
    /** 订单已确认，等待顾客入住。 */
    PENDING_CHECKIN,
    /** 顾客已办理入住。 */
    CHECKED_IN,
    /** 顾客已退房，订单完成。 */
    COMPLETED,
    /** 订单已取消。 */
    CANCELLED
}
