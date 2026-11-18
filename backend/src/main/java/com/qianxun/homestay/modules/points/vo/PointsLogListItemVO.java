package com.qianxun.homestay.modules.points.vo;

import java.time.OffsetDateTime;

/** 积分流水列表项；金额始终以十进制字符串返回。 */
public record PointsLogListItemVO(
        /** 积分流水 ID。 */
        Long id,
        /** 流水归属会员 ID。 */
        Long memberId,
        /** 关联订单 ID；无关联时为空。 */
        Long orderId,
        /** 稳定英文积分流水类型。 */
        String type,
        /** 可用积分变动值，可正可负。 */
        Integer points,
        /** 成长积分变动值，可正可负。 */
        Integer growthPoints,
        /** 流水关联金额，单位元的十进制字符串。 */
        String amount,
        /** 积分抵扣或返还金额，单位元的十进制字符串。 */
        String discountAmount,
        /** 本次流水完成后的可用积分余额。 */
        Integer balanceAfter,
        /** 发放、抵扣或返还的说明。 */
        String remark,
        /** 流水创建时间。 */
        OffsetDateTime createdAt) {
}
