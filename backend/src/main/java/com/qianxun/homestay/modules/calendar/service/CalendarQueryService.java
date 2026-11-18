package com.qianxun.homestay.modules.calendar.service;

import com.qianxun.homestay.modules.calendar.vo.CalendarVO;
import java.time.YearMonth;

/** 后台房态日历查询服务。 */
public interface CalendarQueryService {

    /** 查询指定月份的客房、订单和封房日历。 */
    CalendarVO findMonth(YearMonth month);
}
