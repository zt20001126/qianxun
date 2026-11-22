package com.qianxun.homestay.modules.mall.domain;

import java.math.BigDecimal;

/** 商城微信退款请求的持久化快照。 */
public record MallWechatRefundCommand(
        /** 商户退款号。 */ String refundNo,
        /** 商户支付号。 */ String paymentNo,
        /** 微信支付交易号。 */ String transactionId,
        /** 退款金额。 */ BigDecimal refundAmount,
        /** 原支付金额。 */ BigDecimal paymentAmount,
        /** 已知微信退款号。 */ String wechatRefundId,
        /** 商城订单编号。 */ Long orderId,
        /** 当前退款状态。 */ String status) {
}
