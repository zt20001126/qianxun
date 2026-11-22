package com.qianxun.homestay.modules.mall.vo;

/** 商城支付状态及已创建的订单标识。 */
public record MallPaymentResultVO(
        /** 支付单号。 */ String paymentNo,
        /** 支付状态。 */ String status,
        /** 商城订单编号。 */ Long orderId,
        /** 商城订单号。 */ String orderNo,
        /** 交易金额，单位元。 */ String amount) {
}
