package com.qianxun.homestay.modules.roomblock.vo;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/** R10-R12 后台封房视图，不包含订单或顾客信息。 */
public record RoomBlockVO(
        /** 封房记录编号。 */
        Long id,
        /** 被封闭的客房编号。 */
        Long roomId,
        /** 封房开始日期，包含当天。 */
        LocalDate startDate,
        /** 封房结束日期，不包含当天。 */
        LocalDate endDate,
        /** 管理员填写的封房原因。 */
        String reason,
        /** 封房记录创建时间，包含时区偏移。 */
        OffsetDateTime createdAt,
        /** 封房记录最近更新时间，包含时区偏移。 */
        OffsetDateTime updatedAt) {
}
