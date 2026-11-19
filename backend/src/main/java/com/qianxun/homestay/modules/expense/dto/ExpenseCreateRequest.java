package com.qianxun.homestay.modules.expense.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/** 新增经营支出时接收的接口请求。 */
public record ExpenseCreateRequest(
        /** 支出内容，由管理员自由填写。 */
        @NotBlank(message = "支出内容不能为空")
        @Size(max = 300, message = "支出内容不能超过 300 个字符")
        String content,
        /** 实际支出金额，接口以十进制数值传递。 */
        @NotNull(message = "支出金额不能为空")
        @DecimalMin(value = "0.01", message = "支出金额必须大于零")
        @Digits(integer = 10, fraction = 2, message = "支出金额最多两位小数")
        BigDecimal amount,
        /** 实际发生支出的日期，不是记录创建日期。 */
        @NotNull(message = "支出日期不能为空")
        LocalDate date) {
}
