package com.qianxun.homestay.modules.mall.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** 商城预支付时的商品和收货资料。 */
public record MallPrepayRequest(
        /** 商品编号。 */ @NotNull @Positive Long productId,
        /** 购买数量。 */ @NotNull @Positive @Max(100000) Integer quantity,
        /** 申请使用的积分数量。 */ @PositiveOrZero Integer pointsUsed,
        /** 收货人姓名。 */ @NotBlank @Size(max = 80) String recipientName,
        /** 收货手机号。 */ @NotBlank @Pattern(regexp = "^1[3-9]\\d{9}$") String recipientPhone,
        /** 收货地址。 */ @NotBlank @Size(max = 500) String shippingAddress) {
}
