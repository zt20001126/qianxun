package com.qianxun.homestay.modules.expense.controller;

import com.qianxun.homestay.common.api.ApiResponse;
import com.qianxun.homestay.modules.expense.dto.ExpenseCreateRequest;
import com.qianxun.homestay.modules.expense.service.ExpenseService;
import com.qianxun.homestay.modules.expense.vo.ExpensePageVO;
import com.qianxun.homestay.modules.expense.vo.ExpenseSummaryVO;
import com.qianxun.homestay.modules.expense.vo.ExpenseVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** E01–E06 后台经营支出接口；仅提供查询和新增。 */
@RestController
@Validated
@RequestMapping("/api/admin/expenses")
public class AdminExpenseController {

    private final ExpenseService expenseService;

    public AdminExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    /** 按日期和关键词分页读取支出记录。 */
    @GetMapping
    public ApiResponse<ExpensePageVO> list(
            @RequestParam(name = "start_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "end_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "1") @Min(1) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(expenseService.list(startDate, endDate, keyword, page, size));
    }

    /** 查询单笔支出详情。 */
    @GetMapping("/{id}")
    public ApiResponse<ExpenseVO> get(@PathVariable @Min(1) Long id) {
        return ApiResponse.success(expenseService.get(id));
    }

    /** 新增经营支出；支出记录创建后不可编辑或删除。 */
    @PostMapping
    public ApiResponse<ExpenseVO> create(@Valid @RequestBody ExpenseCreateRequest request) {
        return ApiResponse.success(expenseService.create(request));
    }

    /** 汇总指定日期区间的实际支出。 */
    @GetMapping("/summary")
    public ApiResponse<ExpenseSummaryVO> summarize(
            @RequestParam(name = "start_date")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "end_date")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.success(expenseService.summarize(startDate, endDate));
    }
}
