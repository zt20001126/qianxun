package com.qianxun.homestay.modules.payment.vo;

import com.qianxun.homestay.modules.payment.enums.PaymentStatus;
import java.time.OffsetDateTime;

/** P04 后台支付流水列表项。 */
public record PaymentListItemVO(
        Long id,
        String paymentNo,
        Long orderId,
        String amount,
        PaymentStatus status,
        OffsetDateTime paidAt) {
}
