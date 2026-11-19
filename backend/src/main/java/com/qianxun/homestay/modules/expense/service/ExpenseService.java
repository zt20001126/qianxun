package com.qianxun.homestay.modules.expense.service;

import com.qianxun.homestay.modules.expense.dto.ExpenseCreateRequest;
import com.qianxun.homestay.modules.expense.vo.ExpensePageVO;
import com.qianxun.homestay.modules.expense.vo.ExpenseSummaryVO;
import com.qianxun.homestay.modules.expense.vo.ExpenseVO;
import java.time.LocalDate;

/** 经营支出查询、新增及统计业务接口。 */
public interface ExpenseService {

    /** 按可选日期范围和关键词分页查询经营支出。 */
    ExpensePageVO list(LocalDate startDate, LocalDate endDate, String keyword, int page, int size);

    /** 查询指定支出详情；不存在时返回 404。 */
    ExpenseVO get(Long id);

    /** 新增一笔支出，成功后返回数据库生成的记录。 */
    ExpenseVO create(ExpenseCreateRequest request);

    /** 汇总指定闭区间内的支出总额、笔数及逐日金额。 */
    ExpenseSummaryVO summarize(LocalDate startDate, LocalDate endDate);
}
