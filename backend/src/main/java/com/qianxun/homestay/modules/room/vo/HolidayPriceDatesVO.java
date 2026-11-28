package com.qianxun.homestay.modules.room.vo;

import java.time.LocalDate;
import java.util.List;

/** 管理后台月度节假日日期清单。 */
public record HolidayPriceDatesVO(
        /** 已启用节假日价的日期，按日期升序。 */
        List<LocalDate> dates) {
}
