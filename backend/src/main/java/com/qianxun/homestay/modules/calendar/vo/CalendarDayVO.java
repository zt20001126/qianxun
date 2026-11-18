package com.qianxun.homestay.modules.calendar.vo;

import com.qianxun.homestay.modules.calendar.enums.CalendarCellStatus;
import java.time.LocalDate;

/** 后台月历中的单日房态格子。 */
public record CalendarDayVO(
        /** 房态对应的住宿日期。 */
        LocalDate date,
        /** 房态编码；连续住宿后续夜晚使用 OCCUPIED 展示标记。 */
        CalendarCellStatus status,
        /** 占用该日期的订单编号；空房或封房时为空。 */
        Long orderId,
        /** 占用该日期的封房记录编号；非封房日期时为空。 */
        Long blockId) {
}
