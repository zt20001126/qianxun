package com.qianxun.homestay.modules.expense.vo;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/** 经营支出列表及详情使用的视图对象。 */
public record ExpenseVO(
        /** 支出记录唯一标识。 */
        Long id,
        /** 管理员填写的支出内容。 */
        String content,
        /** 支出金额，固定保留两位小数。 */
        String amount,
        /** 实际支出日期。 */
        LocalDate date,
        /** 支出记录创建时间。 */
        OffsetDateTime createdAt,
        /** 支出记录更新时间。 */
        OffsetDateTime updatedAt) {
}
