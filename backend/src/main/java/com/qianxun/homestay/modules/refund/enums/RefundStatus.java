package com.qianxun.homestay.modules.refund.enums;

/** 退款流水在数据库中持久化的稳定状态编码。 */
public enum RefundStatus {
    /** 微信已受理但尚未确认完成。 */
    PENDING,
    /** 退款已由支付渠道确认成功。 */
    SUCCESS,
    /** 退款处理失败。 */
    FAILED
}
