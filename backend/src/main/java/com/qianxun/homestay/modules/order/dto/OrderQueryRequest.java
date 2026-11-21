package com.qianxun.homestay.modules.order.dto;

import com.qianxun.homestay.modules.order.enums.OrderStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;

/** 后台订单列表的筛选和分页条件。 */
public record OrderQueryRequest(
        /** 按订单来源渠道编号筛选。 */
        Long channelId,
        /** 按客房编号筛选。 */
        Long roomId,
        /** 按达人编号筛选。 */
        Long creatorId,
        /** 按订单状态筛选。 */
        OrderStatus status,
        /** 入住日期筛选起始值，包含当天。 */
        LocalDate checkInFrom,
        /** 入住日期筛选结束值，包含当天。 */
        LocalDate checkInTo,
        /** 按预订人姓名模糊筛选。 */
        String guestName,
        /** 按预订手机号筛选。 */
        String phone,
        /** 查询页码，从 1 开始。 */
        @Min(1) int page,
        /** 每页记录数，最大为 100。 */
        @Min(1) @Max(100) int size,
        /** 订单号、预订人或手机号关键词。 */
        String keyword,
        /** 仅筛选无达人订单。 */
        boolean withoutCreator) {
}
