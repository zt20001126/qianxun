package com.qianxun.homestay.modules.booking.enums;

/** 临时预订锁房记录的持久化状态。 */
public enum BookingLockStatus {
    /** 等待模拟支付，且在有效期内占用房间和预占积分。 */
    PENDING,
    /** 支付成功并已转换为正式订单。 */
    PAID,
    /** 超过有效期后关闭并释放占用。 */
    EXPIRED,
    /** 交易被主动关闭。 */
    CANCELLED
}
