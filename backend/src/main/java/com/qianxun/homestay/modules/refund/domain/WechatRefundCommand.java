package com.qianxun.homestay.modules.refund.domain;

import java.math.BigDecimal;
import com.qianxun.homestay.modules.refund.enums.RefundStatus;

/** 事务准备后交由微信支付网关处理的退款参数。 */
public record WechatRefundCommand(
        Long refundId,
        String refundNo,
        String paymentNo,
        BigDecimal refundAmount,
        BigDecimal paymentAmount,
        String reason,
        String wechatRefundId,
        RefundStatus status) {
}
