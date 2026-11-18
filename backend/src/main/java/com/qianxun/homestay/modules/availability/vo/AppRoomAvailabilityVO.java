package com.qianxun.homestay.modules.availability.vo;

import java.time.LocalDate;
import java.util.List;

/** R03 月度日历所需的不可订住宿夜晚。 */
public record AppRoomAvailabilityVO(
        /** 客房数据库编号。 */
        Long roomId,
        /** 查询月份，格式为 YYYY-MM。 */
        String month,
        /** 不可预订的入住日期列表。 */
        List<LocalDate> unavailableDates) {
}
