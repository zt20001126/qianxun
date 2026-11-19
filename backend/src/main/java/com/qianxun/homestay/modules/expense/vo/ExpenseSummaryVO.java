package com.qianxun.homestay.modules.expense.vo;

import java.util.List;

/** 指定日期区间的经营支出统计结果。 */
public record ExpenseSummaryVO(
        /** 区间内支出金额合计，固定保留两位小数。 */
        String totalAmount,
        /** 区间内支出笔数。 */
        long count,
        /** 按实际支出日期汇总的每日金额。 */
        List<ExpenseDailySummaryVO> daily) {
}
