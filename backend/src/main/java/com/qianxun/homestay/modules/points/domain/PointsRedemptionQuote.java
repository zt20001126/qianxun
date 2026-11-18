package com.qianxun.homestay.modules.points.domain;

import java.math.BigDecimal;

/** 积分抵扣试算的内部结果，不直接作为数据库映射对象。 */
public record PointsRedemptionQuote(
        /** 房费原价。 */
        BigDecimal roomAmount,
        /** 扣除其他有效锁房预占后的可用积分。 */
        int availablePoints,
        /** 当前会员等级每元所需积分。 */
        int pointsPerYuan,
        /** 当前房价及可用积分共同允许使用的最大积分。 */
        int maxPointsUsable,
        /** 本次请求使用的积分数量。 */
        int pointsUsed,
        /** 积分抵扣金额，向下保留到分。 */
        BigDecimal discountAmount,
        /** 积分抵扣后的待支付金额。 */
        BigDecimal payAmount) {
}
