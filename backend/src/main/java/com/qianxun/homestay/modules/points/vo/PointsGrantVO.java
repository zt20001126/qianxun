package com.qianxun.homestay.modules.points.vo;

/** 管理员发放积分后的流水结果。 */
public record PointsGrantVO(
        /** 发放积分的会员 ID。 */
        Long memberId,
        /** 本次增加的可用积分。 */
        Integer points,
        /** 本次增加的成长积分；赠送类型为零。 */
        Integer growthPoints,
        /** 本次发放后的可用积分余额。 */
        Integer balanceAfter,
        /** 对应积分流水 ID。 */
        Long logId) {
}
