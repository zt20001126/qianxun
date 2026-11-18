package com.qianxun.homestay.modules.calendar.vo;

import java.util.List;

/** 后台月历中的单间客房及其每日房态。 */
public record CalendarRoomVO(
        /** 客房数据库编号。 */
        Long roomId,
        /** 客房对外房号。 */
        String roomNumber,
        /** 当前月份内按日期升序排列的房态格子。 */
        List<CalendarDayVO> days) {
}
