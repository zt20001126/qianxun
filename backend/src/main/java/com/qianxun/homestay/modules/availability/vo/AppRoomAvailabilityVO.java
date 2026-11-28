package com.qianxun.homestay.modules.availability.vo;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** R03 单房月历的不可订日期及可订夜价。 */
public record AppRoomAvailabilityVO(
        /** 客房数据库编号。 */
        Long roomId,
        /** 查询月份，格式为 YYYY-MM。 */
        String month,
        /** 不可预订的入住日期列表。 */
        List<LocalDate> unavailableDates,
        /** 按入住日期索引的可订夜价，金额为两位小数的十进制字符串。 */
        Map<LocalDate, String> nightlyPrices) {
}
