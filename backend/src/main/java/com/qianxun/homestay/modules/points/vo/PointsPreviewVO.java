package com.qianxun.homestay.modules.points.vo;

/** T03 积分抵扣试算响应；最终金额仍以 P01 服务端复核为准。 */
public record PointsPreviewVO(
        /** 房费原价，十进制字符串，单位为元。 */
        String roomAmount,
        /** 扣除其他有效锁房预占后的可用积分。 */
        Integer availablePoints,
        /** 当前会员等级每元所需积分。 */
        Integer pointsPerYuan,
        /** 当前房价和可用积分允许使用的最大积分。 */
        Integer maxPointsUsable,
        /** 本次请求积分数量。 */
        Integer pointsUsed,
        /** 积分抵扣金额，十进制字符串，单位为元。 */
        String discountAmount,
        /** 积分抵扣后的应付金额，十进制字符串，单位为元。 */
        String payAmount) {
}
