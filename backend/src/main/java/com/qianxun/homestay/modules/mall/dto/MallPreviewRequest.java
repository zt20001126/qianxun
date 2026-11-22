package com.qianxun.homestay.modules.mall.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/** 商城购买数量和积分试算请求。 */
public record MallPreviewRequest(
        /** 商品编号。 */ @NotNull @Positive Long productId,
        /** 购买数量。 */ @NotNull @Positive @Max(100000) Integer quantity,
        /** 请求使用的积分。 */ @PositiveOrZero Integer pointsUsed) {
}
