package com.qianxun.homestay.modules.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** P01 创建待支付交易并临时锁房的请求。 */
public record PrepayRequest(
        /** 预订客房编号。 */
        @NotNull @Positive Long roomId,
        /** 预定入住日期。 */
        @NotNull LocalDate checkIn,
        /** 预定退房日期，不占用当天房晚。 */
        @NotNull LocalDate checkOut,
        /** 预订联系人姓名。 */
        @NotBlank @Size(max = 80) String guestName,
        /** 预订联系人手机号。 */
        @NotBlank @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确") String phone,
        /** 入住人数，必须不超过客房容量。 */
        @NotNull @Positive Integer guestCount,
        /** 顾客备注。 */
        @Size(max = 500) String remark,
        /** 申请抵扣的积分数量，省略时按零积分处理。 */
        @PositiveOrZero Integer pointsUsed) {
}
