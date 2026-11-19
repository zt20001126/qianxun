package com.qianxun.homestay.modules.expense.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 经营支出持久化实体，对应 expenses 表。 */
public class ExpenseEntity {

    /** 支出记录唯一标识。 */
    private Long id;
    /** 支出内容。 */
    private String content;
    /** 实际支出金额，单位为元。 */
    private BigDecimal amount;
    /** 实际支出归属日期。 */
    private LocalDate date;
    /** 记录创建时间。 */
    private LocalDateTime createdAt;
    /** 记录最近更新时间；支出创建后不提供更新接口。 */
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
