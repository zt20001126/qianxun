package com.qianxun.homestay.modules.roomblock.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** R11/R12 封房新增及修改请求。 */
public record RoomBlockSaveRequest(
        @NotNull(message = "请选择房间") @Positive(message = "房间编号不正确") Long roomId,
        @NotNull(message = "请选择封房开始日期") LocalDate startDate,
        @NotNull(message = "请选择封房结束日期") LocalDate endDate,
        @Size(max = 200, message = "封房原因不能超过 200 个字符") String reason) {
}
