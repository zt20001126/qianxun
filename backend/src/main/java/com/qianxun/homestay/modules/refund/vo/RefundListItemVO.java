package com.qianxun.homestay.modules.refund.vo;

import com.qianxun.homestay.modules.refund.enums.RefundStatus;
import java.time.OffsetDateTime;

/** P05 后台退款流水列表项。 */
public record RefundListItemVO(
        Long id,
        String refundNo,
        Long orderId,
        String amount,
        RefundStatus status,
        String reason,
        OffsetDateTime refundedAt) {
}
