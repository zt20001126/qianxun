package com.qianxun.homestay.modules.report.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;

/** 经营总览中所选月份的订单、支出及利润指标。 */
public record DashboardMonthSummaryVO(
        /** 未入住订单的实付营业额。 */
        String unoccupiedOrderRevenue,
        /** 已入住或已完成订单的实付营业额。 */
        String checkedInOrderRevenue,
        /** 平台手续费。 */
        String platformFee,
        /** 平台达人提成。 */
        String platformCommission,
        /** 达人额外提成。 */
        String extraCommission,
        /** 全部有效订单的商家到手金额。 */
        String netAmount,
        /** 本月实际经营支出。 */
        String expenseAmount,
        /** 已入住订单到手金额减本月经营支出。 */
        String operatingProfit,
        /** 已入住间夜除以可售间夜；分母为零时为空。 */
        @JsonInclude(JsonInclude.Include.ALWAYS)
        BigDecimal occupancyRate) {
}
