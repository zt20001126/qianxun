package com.qianxun.homestay.modules.report.service;

import com.qianxun.homestay.modules.report.enums.ReportScope;
import com.qianxun.homestay.modules.report.vo.DashboardVO;
import com.qianxun.homestay.modules.report.vo.ReportDimensionVO;
import com.qianxun.homestay.modules.report.vo.ReportOverviewVO;
import com.qianxun.homestay.modules.report.vo.RoomReportDimensionVO;
import java.time.LocalDate;
import java.time.YearMonth;

/** 经营总览与月度四维报表业务接口。 */
public interface ReportService {

    /** 查询指定日期房态及指定月份的经营总览。 */
    DashboardVO dashboard(LocalDate date, YearMonth month);

    /** 按订单状态范围汇总指定月份的整体经营数据。 */
    ReportOverviewVO overview(YearMonth month, ReportScope scope);

    /** 按订单来源统计指定月份的住宿夜晚与金额。 */
    ReportDimensionVO channels(YearMonth month, ReportScope scope, Long channelId);

    /** 按达人统计指定月份的住宿夜晚与金额，包含无达人分组。 */
    ReportDimensionVO creators(YearMonth month, ReportScope scope, Long creatorId);

    /** 按客房统计指定月份的住宿夜晚、金额及入住率。 */
    RoomReportDimensionVO rooms(YearMonth month, ReportScope scope, Long roomId);
}
