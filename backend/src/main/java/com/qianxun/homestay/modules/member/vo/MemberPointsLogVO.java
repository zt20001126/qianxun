package com.qianxun.homestay.modules.member.vo;

import java.time.OffsetDateTime;

/** 后台会员详情中的关联积分流水摘要。 */
public record MemberPointsLogVO(
        /** 积分流水 ID。 */
        Long id,
        /** 流水归属会员 ID。 */
        Long memberId,
        /** 关联订单 ID；非订单流水为空。 */
        Long orderId,
        /** 积分流水稳定类型编码。 */
        String type,
        /** 可用积分变动数量。 */
        Integer points,
        /** 成长积分变动数量。 */
        Integer growthPoints,
        /** 流水关联金额，单位元。 */
        String amount,
        /** 积分抵扣或返还金额，单位元。 */
        String discountAmount,
        /** 流水完成后的可用积分余额。 */
        Integer balanceAfter,
        /** 流水业务说明。 */
        String remark,
        /** 流水创建时间。 */
        OffsetDateTime createdAt) {
}
