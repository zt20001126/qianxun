package com.qianxun.homestay.modules.report.vo;

import com.fasterxml.jackson.annotation.JsonInclude;

/** 来源、达人或客房报表中的单项统计。 */
public record ReportDimensionItemVO(
        /** 分组业务主键；无达人分组时为空。 */
        @JsonInclude(JsonInclude.Include.ALWAYS)
        Long id,
        /** 分组展示名称。 */
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
        String netAmount) {
}
