package com.qianxun.homestay.modules.roomblock.vo;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/** R10-R12 后台封房视图，不包含订单或顾客信息。 */
public record RoomBlockVO(
        Long id,
        Long roomId,
        LocalDate startDate,
        LocalDate endDate,
        String reason,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
