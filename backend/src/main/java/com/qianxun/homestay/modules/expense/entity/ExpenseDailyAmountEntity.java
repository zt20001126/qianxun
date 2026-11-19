package com.qianxun.homestay.modules.expense.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 支出日期分组查询结果，不对应独立数据库表。 */
public class ExpenseDailyAmountEntity {

    /** 实际支出日期。 */
    private LocalDate date;
    /** 当日支出合计。 */
    private BigDecimal amount;

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
