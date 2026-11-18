package com.qianxun.homestay.modules.calendar.controller;

import com.qianxun.homestay.common.api.ApiResponse;
import com.qianxun.homestay.modules.calendar.service.CalendarQueryService;
import com.qianxun.homestay.modules.calendar.vo.CalendarVO;
import jakarta.validation.constraints.Pattern;
import java.time.YearMonth;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** K01 后台月度房态日历接口。 */
@RestController
@Validated
@RequestMapping("/api/admin/room-calendar")
public class AdminCalendarController {

    private final CalendarQueryService calendarQueryService;

    public AdminCalendarController(CalendarQueryService calendarQueryService) {
        this.calendarQueryService = calendarQueryService;
    }

    /** 按 YYYY-MM 查询所有客房当月每日订单与封房状态。 */
    @GetMapping
    public ApiResponse<CalendarVO> findMonth(
            @RequestParam("month") @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "月份格式应为 YYYY-MM")
            String month) {
        return ApiResponse.success(calendarQueryService.findMonth(YearMonth.parse(month)));
    }
}
