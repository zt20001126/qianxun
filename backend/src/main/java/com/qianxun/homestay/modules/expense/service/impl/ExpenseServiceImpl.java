package com.qianxun.homestay.modules.expense.service.impl;

import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.modules.expense.dto.ExpenseCreateRequest;
import com.qianxun.homestay.modules.expense.entity.ExpenseDailyAmountEntity;
import com.qianxun.homestay.modules.expense.entity.ExpenseEntity;
import com.qianxun.homestay.modules.expense.mapper.ExpenseMapper;
import com.qianxun.homestay.modules.expense.service.ExpenseService;
import com.qianxun.homestay.modules.expense.vo.ExpenseDailySummaryVO;
import com.qianxun.homestay.modules.expense.vo.ExpensePageVO;
import com.qianxun.homestay.modules.expense.vo.ExpenseSummaryVO;
import com.qianxun.homestay.modules.expense.vo.ExpenseVO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 经营支出业务实现，统一处理范围校验、分页及金额格式。 */
@Service
public class ExpenseServiceImpl implements ExpenseService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final BigDecimal ZERO_AMOUNT = new BigDecimal("0.00");

    private final ExpenseMapper expenseMapper;

    public ExpenseServiceImpl(ExpenseMapper expenseMapper) {
        this.expenseMapper = expenseMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public ExpensePageVO list(LocalDate startDate, LocalDate endDate, String keyword, int page, int size) {
        validateRange(startDate, endDate);
        int actualPage = page < 1 ? 1 : page;
        int actualSize = size < 1 ? DEFAULT_PAGE_SIZE : Math.min(size, 100);
        String actualKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        long offset = (long) (actualPage - 1) * actualSize;
        List<ExpenseVO> items = expenseMapper.findPage(startDate, endDate, actualKeyword, offset, actualSize)
                .stream()
                .map(this::toVO)
                .toList();
        return new ExpensePageVO(items, expenseMapper.count(startDate, endDate, actualKeyword),
                actualPage, actualSize);
    }

    @Override
    @Transactional(readOnly = true)
    public ExpenseVO get(Long id) {
        ExpenseEntity expense = expenseMapper.findById(id);
        if (expense == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "经营支出记录不存在");
        }
        return toVO(expense);
    }

    @Override
    @Transactional
    public ExpenseVO create(ExpenseCreateRequest request) {
        // 请求金额已经完成 Jakarta Validation；在写入前统一保存为两位小数。
        ExpenseEntity expense = new ExpenseEntity();
        expense.setContent(request.content().trim());
        expense.setAmount(request.amount().setScale(2, RoundingMode.UNNECESSARY));
        expense.setDate(request.date());
        expenseMapper.insert(expense);
        return get(expense.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public ExpenseSummaryVO summarize(LocalDate startDate, LocalDate endDate) {
        validateRequiredRange(startDate, endDate);
        BigDecimal total = expenseMapper.sumBetween(startDate, endDate);
        List<ExpenseDailySummaryVO> daily = expenseMapper.sumDailyBetween(startDate, endDate).stream()
                .map(this::toDailyVO)
                .toList();
        return new ExpenseSummaryVO(money(total), expenseMapper.countBetween(startDate, endDate), daily);
    }

    private void validateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "开始日期不能晚于结束日期");
        }
    }

    private void validateRequiredRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "开始日期和结束日期不能为空");
        }
        validateRange(startDate, endDate);
    }

    private ExpenseVO toVO(ExpenseEntity expense) {
        return new ExpenseVO(expense.getId(), expense.getContent(), money(expense.getAmount()),
                expense.getDate(), expense.getCreatedAt().atZone(BUSINESS_ZONE).toOffsetDateTime(),
                expense.getUpdatedAt().atZone(BUSINESS_ZONE).toOffsetDateTime());
    }

    private ExpenseDailySummaryVO toDailyVO(ExpenseDailyAmountEntity daily) {
        return new ExpenseDailySummaryVO(daily.getDate(), money(daily.getAmount()));
    }

    private String money(BigDecimal amount) {
        return (amount == null ? ZERO_AMOUNT : amount).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
