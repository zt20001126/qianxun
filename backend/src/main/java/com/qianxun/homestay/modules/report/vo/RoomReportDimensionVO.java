package com.qianxun.homestay.modules.report.vo;

import com.qianxun.homestay.modules.report.enums.ReportScope;
import java.util.List;

/** S05 客房维度月报响应。 */
public record RoomReportDimensionVO(
        /** 报表所属月份。 */
        String month,
        /** 当前订单状态范围。 */
        ReportScope scope,
        /** 按客房分组的统计行。 */
        List<RoomReportDimensionItemVO> list) {
}
