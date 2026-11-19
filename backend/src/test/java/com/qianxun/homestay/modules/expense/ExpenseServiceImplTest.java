package com.qianxun.homestay.modules.expense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.modules.expense.entity.ExpenseDailyAmountEntity;
import com.qianxun.homestay.modules.expense.mapper.ExpenseMapper;
import com.qianxun.homestay.modules.expense.service.impl.ExpenseServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/** 经营支出汇总与日期范围校验的回归测试。 */
class ExpenseServiceImplTest {

    private final ExpenseMapper expenseMapper = mock(ExpenseMapper.class);
    private final ExpenseServiceImpl service = new ExpenseServiceImpl(expenseMapper);

    @Test
    void summarizesByActualExpenseDateAndFormatsMoneyToTwoDecimals() {
        LocalDate start = LocalDate.of(2026, 11, 1);
        LocalDate end = LocalDate.of(2026, 11, 30);
        ExpenseDailyAmountEntity firstDay = new ExpenseDailyAmountEntity();
        firstDay.setDate(LocalDate.of(2026, 11, 3));
        firstDay.setAmount(new BigDecimal("12.30"));
        when(expenseMapper.sumBetween(start, end)).thenReturn(new BigDecimal("12.30"));
        when(expenseMapper.countBetween(start, end)).thenReturn(1L);
        when(expenseMapper.sumDailyBetween(start, end)).thenReturn(List.of(firstDay));

        var summary = service.summarize(start, end);

        assertThat(summary.totalAmount()).isEqualTo("12.30");
        assertThat(summary.count()).isEqualTo(1);
        assertThat(summary.daily()).containsExactly(
                new com.qianxun.homestay.modules.expense.vo.ExpenseDailySummaryVO(
                        LocalDate.of(2026, 11, 3), "12.30"));
    }

    @Test
    void rejectsReversedDateRange() {
        assertThatThrownBy(() -> service.summarize(
                LocalDate.of(2026, 11, 30), LocalDate.of(2026, 11, 1)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
