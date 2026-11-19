package com.qianxun.homestay.modules.expense.vo;

import java.time.LocalDate;

/** 单日实际经营支出汇总。 */
public record ExpenseDailySummaryVO(
        /** 支出实际归属日期。 */
        LocalDate date,
        /** 当日支出金额，固定保留两位小数。 */
        String amount) {
}
