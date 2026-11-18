package com.qianxun.homestay.modules.points.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;

/** T03 预览指定房间日期和积分抵扣金额的请求。 */
public record PointsPreviewRequest(
        /** 预订客房编号。 */
        @NotNull @Positive Long roomId,
        /** 预定入住日期。 */
        @NotNull LocalDate checkIn,
        /** 预定退房日期，不占用当天房晚。 */
        @NotNull LocalDate checkOut,
        /** 申请抵扣积分，省略时按零积分处理。 */
        @PositiveOrZero Integer pointsUsed) {
}
