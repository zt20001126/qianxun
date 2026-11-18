package com.qianxun.homestay.modules.points.vo;

/** 按需重试已完成订单积分奖励的处理结果。 */
public record PointsRewardRetryVO(
        /** 重试的订单 ID。 */
        Long orderId,
        /** 是否已存在或成功生成订单奖励流水。 */
        boolean rewarded,
        /** 对应订单奖励流水 ID；未奖励时为空。 */
        Long logId) {
}
