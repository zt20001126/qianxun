package com.qianxun.homestay.modules.payment.vo;

import com.qianxun.homestay.modules.payment.enums.PaymentStatus;

/** P08 模拟支付成功及正式订单关联结果。 */
public record MockPayVO(
        /** 商户支付单号。 */
        String paymentNo,
        /** 模拟支付成功状态。 */
        PaymentStatus status,
        /** 新建或已存在的正式订单编号。 */
        Long orderId,
        /** 新建或已存在的系统订单号。 */
        String orderNo,
        /** 模拟实付金额，十进制字符串，单位为元。 */
        String paidAmount) {
}
