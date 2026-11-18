package com.qianxun.homestay.modules.order.enums;

/** 管理后台允许执行的订单状态动作。 */
public enum OrderAction {
    /** 确认待确认订单。 */
    CONFIRM,
    /** 为已确认订单办理入住。 */
    CHECK_IN,
    /** 为已入住订单办理退房并完成订单。 */
    CHECK_OUT,
    /** 取消允许取消的订单。 */
    CANCEL
}
