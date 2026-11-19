package com.qianxun.homestay.modules.report.vo;

import java.time.LocalDate;

/** 经营总览中的单间客房指定日期房态。 */
public record DashboardRoomVO(
        /** 房间唯一标识。 */
        Long roomId,
        /** 房间号。 */
        String roomNumber,
        /** 房间名称。 */
        String roomName,
        /** 当日房态编码；连住后续夜晚使用 OCCUPIED 标记。 */
        String status,
        /** 当日覆盖的订单唯一标识。 */
        Long orderId,
        /** 预订联系人姓名。 */
        String guestName,
        /** 订单登记的入住人数。 */
        Integer guestCount,
        /** 订单入住日期。 */
        LocalDate checkIn,
        /** 订单退房日期。 */
        LocalDate checkOut,
        /** 订单来源名称。 */
        String channelName) {
}
