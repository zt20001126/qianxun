package com.qianxun.homestay.modules.mall.vo;

/** 商城整单退款结果。 */
public record MallRefundVO(
        /** 商户退款单号。 */ String refundNo,
        /** 商城订单编号。 */ Long orderId,
        /** 退款状态。 */ String status,
        /** 退款金额，单位元。 */ String amount) {
}
