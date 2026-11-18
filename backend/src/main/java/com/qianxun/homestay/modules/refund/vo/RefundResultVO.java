package com.qianxun.homestay.modules.refund.vo;

import com.qianxun.homestay.modules.refund.enums.RefundStatus;

/** P03 自助模拟退款结果。 */
public record RefundResultVO(Long refundId, String refundNo, RefundStatus status) {
}
