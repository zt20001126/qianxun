package com.qianxun.homestay.modules.room.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/** 后台提交的单月节假日日期变更。 */
public record HolidayPriceDatesUpdateRequest(
        /** 变更所属月份，格式为 YYYY-MM。 */
        @NotNull @Pattern(regexp = MONTH_PATTERN, message = "月份格式应为 YYYY-MM") String month,
        /** 新选中的日期。 */
        @NotNull @Size(max = MAX_DAYS_PER_MONTH) List<@NotNull LocalDate> addDates,
        /** 取消选中的日期。 */
        @NotNull @Size(max = MAX_DAYS_PER_MONTH) List<@NotNull LocalDate> removeDates) {

    /** 月份格式及 MySQL DATE 支持的年份范围。 */
    public static final String MONTH_PATTERN = "[1-9][0-9]{3}-(0[1-9]|1[0-2])";

    private static final int MAX_DAYS_PER_MONTH = 31;
}
