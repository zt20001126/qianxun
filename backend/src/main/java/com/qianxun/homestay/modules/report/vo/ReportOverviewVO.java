package com.qianxun.homestay.modules.report.vo;

import com.qianxun.homestay.modules.report.enums.ReportScope;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;

/** S02 整体月度经营报表。 */
public record ReportOverviewVO(
        /** 报表所属月份。 */
        String month,
        /** 当前订单状态范围。 */
        ReportScope scope,
        /** 入住日期归属本月的订单数。 */
        long orderCount,
        /** 按住宿夜晚归属本月计算的间夜数。 */
        long nightCount,
        /** 当前筛选范围内的顾客实付金额。 */
        String paidAmount,
        /** 当前筛选范围内的平台手续费。 */
        String platformFee,
        /** 当前筛选范围内的平台达人提成。 */
        String platformCommission,
        /** 当前筛选范围内的达人额外提成。 */
        String extraCommission,
        /** 当前筛选范围内的商家到手金额。 */
        String netAmount,
        /** 当前范围中未入住订单的实付营业额。 */
        String unoccupiedOrderRevenue,
        /** 当前范围中已入住或已完成订单的实付营业额。 */
        String checkedInOrderRevenue,
        /** 不受订单范围筛选影响的全月经营支出。 */
        String expenseAmount,
        /** 不受订单范围筛选影响的已入住订单到手金额减全月支出。 */
        String operatingProfit,
        /** 已入住间夜除以可售间夜；分母为零时为空。 */
        @JsonInclude(JsonInclude.Include.ALWAYS)
        BigDecimal occupancyRate) {
}
