package com.qianxun.homestay.modules.report.vo;

import com.qianxun.homestay.modules.report.enums.ReportScope;
import java.util.List;

/** S03–S05 分类月报响应。 */
public record ReportDimensionVO(
        /** 报表所属月份。 */
        String month,
        /** 当前订单状态范围。 */
        ReportScope scope,
        /** 按请求维度分组的统计行。 */
        List<ReportDimensionItemVO> list) {
}
