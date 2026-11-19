package com.qianxun.homestay.modules.report.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;

/** 客房维度报表单行，额外展示客房利润和入住率。 */
public record RoomReportDimensionItemVO(
        /** 客房唯一标识。 */
        Long id,
        /** 房号。 */
        String name,
        /** 入住日期归属本月的订单数。 */
        long orderCount,
        /** 住宿夜晚归属本月的间夜数。 */
        long nightCount,
        /** 顾客实付金额。 */
        String paidAmount,
        /** 平台手续费。 */
        String platformFee,
        /** 平台达人提成。 */
        String platformCommission,
        /** 达人额外提成。 */
        String extraCommission,
        /** 平台达人提成与达人额外提成之和。 */
        String totalCommission,
        /** 商家到手金额。 */
        String netAmount,
        /** 客房利润，仅扣除本房订单费用。 */
        String roomProfit,
        /** 客房入住率；分母为零时显式返回 null。 */
        @JsonInclude(JsonInclude.Include.ALWAYS)
        BigDecimal occupancyRate) {
}
