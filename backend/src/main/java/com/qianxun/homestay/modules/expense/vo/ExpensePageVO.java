package com.qianxun.homestay.modules.expense.vo;

import java.util.List;

/** 经营支出分页结果。 */
public record ExpensePageVO(
        /** 当前页支出记录。 */
        List<ExpenseVO> list,
        /** 符合查询条件的总记录数。 */
        long total,
        /** 当前页码，从 1 开始。 */
        int page,
        /** 每页记录数。 */
        int size) {
}
