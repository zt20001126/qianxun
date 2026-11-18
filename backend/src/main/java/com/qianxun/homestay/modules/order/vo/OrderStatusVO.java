package com.qianxun.homestay.modules.order.vo;

import com.qianxun.homestay.modules.order.enums.OrderStatus;
import java.time.OffsetDateTime;

/** 订单状态动作的结果。 */
public record OrderStatusVO(
        /** 订单数据库编号。 */
        Long id,
        /** 动作执行后的订单状态。 */
        OrderStatus status,
        /** 订单状态最近更新时间，包含时区偏移。 */
        OffsetDateTime updatedAt) {
}
