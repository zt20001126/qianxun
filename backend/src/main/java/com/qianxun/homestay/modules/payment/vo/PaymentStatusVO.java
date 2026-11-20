package com.qianxun.homestay.modules.payment.vo;

import com.qianxun.homestay.modules.payment.enums.PaymentStatus;

/** P02 当前会员的支付交易状态。 */
public record PaymentStatusVO(
        /** 商户支付单号。 */
        String paymentNo,
        /** 支付流水状态。 */
        PaymentStatus status,
        /** 支付成功后生成的订单编号。 */
        Long orderId,
        /** 支付成功后生成的系统订单号。 */
        String orderNo) {
}
