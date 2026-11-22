package com.qianxun.homestay.modules.mall.vo;

/** 会员默认收货资料，仅返回给本人。 */
public record MallShippingVO(
        /** 收货人姓名。 */ String recipientName,
        /** 收货手机号。 */ String recipientPhone,
        /** 收货地址。 */ String shippingAddress) {
}
