package com.qianxun.homestay.modules.mall.vo;

/** 商城商品金额和积分抵扣试算。 */
public record MallPreviewVO(
        /** 商品总额，单位元。 */ String totalAmount,
        /** 扣除其他待支付预占后的积分。 */ int availablePoints,
        /** 每元所需积分。 */ int pointsPerYuan,
        /** 本单最多可抵扣积分。 */ int maxPointsUsable,
        /** 本次使用积分。 */ int pointsUsed,
        /** 抵扣金额，单位元。 */ String discountAmount,
        /** 预计应付金额，单位元。 */ String amount) {
}
