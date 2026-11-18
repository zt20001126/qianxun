package com.qianxun.homestay.modules.refund.enums;

/** 退款流水在数据库中持久化的稳定状态编码。 */
public enum RefundStatus {
    /** 已提交但尚未完成；第一期同步模拟退款不会保留此状态。 */
    PENDING,
    /** 退款已同步模拟成功。 */
    SUCCESS,
    /** 退款处理失败。 */
    FAILED
}
