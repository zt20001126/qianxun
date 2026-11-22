package com.qianxun.homestay.modules.mall.vo;

import java.time.OffsetDateTime;

/** 商城待支付交易与价格快照。 */
public record MallPrepayVO(
        /** 唯一支付单号。 */ String paymentNo,
        /** 交易采用的支付方式。 */ String paymentMethod,
        /** 商品总额，单位元。 */ String totalAmount,
        /** 本次预占积分。 */ int pointsUsed,
        /** 积分抵扣金额，单位元。 */ String discountAmount,
        /** 应付金额，单位元。 */ String amount,
        /** 支付截止时间。 */ OffsetDateTime expiresAt) {
}
