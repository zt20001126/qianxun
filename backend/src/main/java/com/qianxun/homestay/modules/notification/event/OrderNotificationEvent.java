package com.qianxun.homestay.modules.notification.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 支付或退款提交成功后发布的管理员邮件通知事件。 */
public record OrderNotificationEvent(Type type, Long orderId, BigDecimal refundAmount, String reason,
                                     LocalDateTime occurredAt) {
    /** 通知业务类型。 */
    public enum Type { STAY_PAID, STAY_CANCELLED, MALL_PAID, MALL_REFUNDED }
}
