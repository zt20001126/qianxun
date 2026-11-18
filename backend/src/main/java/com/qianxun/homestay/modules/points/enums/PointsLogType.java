package com.qianxun.homestay.modules.points.enums;

/** 积分流水持久化使用的稳定英文类型编码。 */
public enum PointsLogType {
    /** 订单完成后发放的积分奖励。 */
    ORDER_REWARD,
    /** 历史订单补发的积分奖励。 */
    HISTORY_REWARD,
    /** 管理员赠送的可用积分。 */
    GIFT,
    /** 会员兑换抵扣时扣减的积分。 */
    REDEEM,
    /** 订单退款时返还的抵扣积分。 */
    RETURN,
    /** 管理员手动调整积分产生的流水。 */
    ADJUST
}
