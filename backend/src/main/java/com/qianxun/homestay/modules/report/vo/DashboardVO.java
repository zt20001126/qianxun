package com.qianxun.homestay.modules.report.vo;

import java.time.LocalDate;
import java.util.List;

/** S01 当日房态及月度经营总览响应。 */
public record DashboardVO(
        /** 查询的北京时间业务日期。 */
        LocalDate date,
        /** 当日被有效订单占用的房间数。 */
        int occupiedRooms,
        /** 当前经营的客房总数。 */
        int totalRooms,
        /** 每间客房当日的房态及订单摘要。 */
        List<DashboardRoomVO> rooms,
        /** 所选月份的经营指标。 */
        DashboardMonthSummaryVO monthSummary) {
}
