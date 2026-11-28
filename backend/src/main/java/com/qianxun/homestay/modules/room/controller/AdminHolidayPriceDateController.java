package com.qianxun.homestay.modules.room.controller;

import com.qianxun.homestay.common.api.ApiResponse;
import com.qianxun.homestay.modules.room.dto.HolidayPriceDatesUpdateRequest;
import com.qianxun.homestay.modules.room.service.HolidayPriceDateService;
import com.qianxun.homestay.modules.room.vo.HolidayPriceDatesVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.time.YearMonth;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 管理员维护全店节假日计价日期的接口。 */
@RestController
@Validated
@RequestMapping("/api/admin/holiday-price-dates")
public class AdminHolidayPriceDateController {

    private final HolidayPriceDateService service;

    public AdminHolidayPriceDateController(HolidayPriceDateService service) {
        this.service = service;
    }

    /** 查询指定月份的已勾选日期。 */
    @GetMapping
    public ApiResponse<HolidayPriceDatesVO> findMonth(
            @RequestParam("month")
            @Pattern(regexp = HolidayPriceDatesUpdateRequest.MONTH_PATTERN, message = "月份格式应为 YYYY-MM")
            String month) {
        return ApiResponse.success(service.findMonth(YearMonth.parse(month)));
    }

    /** 保存指定月份的勾选和取消勾选。 */
    @PutMapping
    public ApiResponse<HolidayPriceDatesVO> update(@Valid @RequestBody HolidayPriceDatesUpdateRequest request) {
        return ApiResponse.success(service.update(request));
    }
}
