package com.qianxun.homestay.modules.payment.enums;

/** 支付流水的持久化状态。 */
public enum PaymentStatus {
    /** 等待顾客点击模拟支付。 */
    PENDING,
    /** 模拟支付成功。 */
    SUCCESS,
    /** 支付失败。 */
    FAILED,
    /** 待支付交易已关闭。 */
    CLOSED
}
