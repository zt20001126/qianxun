package com.qianxun.homestay.modules.calendar.vo;

import java.util.List;

/** 后台六房月度房态日历响应。 */
public record CalendarVO(
        /** 查询月份，格式为 YYYY-MM。 */
        String month,
        /** 客房及每间客房当月的每日房态。 */
        List<CalendarRoomVO> rooms) {
}
