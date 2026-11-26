package com.qianxun.homestay.modules.report.vo;

import java.time.LocalDate;

/** 指定住宿日期的房态及正式订单摘要。 */
public record RoomDateStatusVO(
        /** 房间号。 */ String roomNumber,
        /** 后台日历房态，连住后续夜为 OCCUPIED。 */ String status,
        /** 查询时的订单状态，无订单时为空。 */ String orderStatus,
        /** 预订联系人姓名，未记录时为空。 */ String guestName,
        /** 订单预定入住日期。 */ LocalDate checkIn,
        /** 订单预定退房日期，不占该日住宿夜。 */ LocalDate checkOut) {
}
