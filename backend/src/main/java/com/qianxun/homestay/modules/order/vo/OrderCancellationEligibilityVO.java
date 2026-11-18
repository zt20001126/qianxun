package com.qianxun.homestay.modules.order.vo;

import java.time.OffsetDateTime;

/** O03 小程序订单自助取消资格查询结果。 */
public record OrderCancellationEligibilityVO(boolean eligible, String reason, OffsetDateTime deadline) {
}
