package com.qianxun.homestay.modules.availability.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

/** R04 小程序日期区间复核请求。 */
public record AppAvailabilityCheckRequest(
        @NotNull(message = "请选择房间") @Positive(message = "房间编号不正确") Long roomId,
        @NotNull(message = "请选择入住日期") LocalDate checkIn,
        @NotNull(message = "请选择退房日期") LocalDate checkOut) {
}
