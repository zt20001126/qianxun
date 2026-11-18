package com.qianxun.homestay.modules.order.domain;

import java.time.LocalDateTime;

/** 订单自助取消资格及对应的北京时间截止时刻。 */
public record OrderCancellationDecision(boolean eligible, String reason, LocalDateTime deadline) {
}
