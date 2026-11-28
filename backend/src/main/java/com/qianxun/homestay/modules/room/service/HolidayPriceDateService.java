package com.qianxun.homestay.modules.room.service;

import com.qianxun.homestay.modules.room.dto.HolidayPriceDatesUpdateRequest;
import com.qianxun.homestay.modules.room.vo.HolidayPriceDatesVO;
import java.time.YearMonth;

/** 全店节假日计价日历维护服务。 */
public interface HolidayPriceDateService {

    /** 查询所选月份已启用节假日价的日期。 */
    HolidayPriceDatesVO findMonth(YearMonth month);

    /** 保存所选月份的勾选变更并返回更新后的日期。 */
    HolidayPriceDatesVO update(HolidayPriceDatesUpdateRequest request);
}
