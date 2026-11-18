package com.qianxun.homestay.modules.member.vo;

import java.time.LocalDate;

/** 后台会员详情中的关联消费订单摘要。 */
public record MemberOrderVO(
        /** 订单 ID。 */
        Long id,
        /** 系统订单号。 */
        String orderNo,
        /** 房间展示编号。 */
        String roomNumber,
        /** 入住日期，包含当晚。 */
        LocalDate checkIn,
        /** 退房日期，不占用当晚。 */
        LocalDate checkOut,
        /** 当前订单状态。 */
        String status,
        /** 订单实付金额，单位元，保留两位小数。 */
        String paidAmount) {
}
