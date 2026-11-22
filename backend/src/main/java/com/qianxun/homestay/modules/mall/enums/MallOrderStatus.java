package com.qianxun.homestay.modules.mall.enums;

/** 商城正式订单状态。 */
public enum MallOrderStatus {
    /** 已付款待发货。 */ PENDING_SHIPMENT,
    /** 已发货。 */ SHIPPED,
    /** 管理员确认完成。 */ COMPLETED,
    /** 退款成功。 */ REFUNDED
}
