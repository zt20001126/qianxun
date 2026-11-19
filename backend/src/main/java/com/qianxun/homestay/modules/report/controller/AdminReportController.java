package com.qianxun.homestay.modules.report.controller;

import com.qianxun.homestay.common.api.ApiResponse;
import com.qianxun.homestay.modules.report.enums.ReportScope;
import com.qianxun.homestay.modules.report.service.ReportService;
import com.qianxun.homestay.modules.report.vo.DashboardVO;
import com.qianxun.homestay.modules.report.vo.ReportDimensionVO;
import com.qianxun.homestay.modules.report.vo.ReportOverviewVO;
import com.qianxun.homestay.modules.report.vo.RoomReportDimensionVO;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** S01–S05 后台经营总览及四维月报接口。 */
@RestController
@Validated
@RequestMapping("/api/admin")
public class AdminReportController {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final ReportService reportService;

    public AdminReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /** 查询指定业务日期的房态和指定月份的经营摘要。 */
    @GetMapping("/dashboard")
    public ApiResponse<DashboardVO> dashboard(
            @RequestParam(name = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(name = "month", required = false)
            @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "月份格式应为 YYYY-MM") String month) {
        LocalDate actualDate = date == null ? LocalDate.now(BUSINESS_ZONE) : date;
        YearMonth actualMonth = month == null ? YearMonth.now(BUSINESS_ZONE) : YearMonth.parse(month);
        return ApiResponse.success(reportService.dashboard(actualDate, actualMonth));
    }

    /** 按订单范围查询整体月度报表。 */
    @GetMapping("/reports/overview")
    public ApiResponse<ReportOverviewVO> overview(
            @RequestParam(name = "month", required = false)
            @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "月份格式应为 YYYY-MM") String month,
            @RequestParam(name = "scope", required = false) ReportScope scope) {
        return ApiResponse.success(reportService.overview(resolveMonth(month), resolveScope(scope)));
    }

    /** 按固定七种订单来源统计月度数据。 */
    @GetMapping("/reports/channels")
    public ApiResponse<ReportDimensionVO> channels(
            @RequestParam(name = "month", required = false)
            @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "月份格式应为 YYYY-MM") String month,
            @RequestParam(name = "scope", required = false) ReportScope scope,
            @RequestParam(name = "channel_id", required = false) @Min(1) Long channelId) {
        return ApiResponse.success(reportService.channels(resolveMonth(month), resolveScope(scope), channelId));
    }

    /** 按达人统计月度数据，并提供无达人订单分组。 */
    @GetMapping("/reports/creators")
    public ApiResponse<ReportDimensionVO> creators(
            @RequestParam(name = "month", required = false)
            @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "月份格式应为 YYYY-MM") String month,
            @RequestParam(name = "scope", required = false) ReportScope scope,
            @RequestParam(name = "creator_id", required = false) @Min(1) Long creatorId) {
        return ApiResponse.success(reportService.creators(resolveMonth(month), resolveScope(scope), creatorId));
    }

    /** 按客房统计月度数据、客房利润及入住率。 */
    @GetMapping("/reports/rooms")
    public ApiResponse<RoomReportDimensionVO> rooms(
            @RequestParam(name = "month", required = false)
            @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "月份格式应为 YYYY-MM") String month,
            @RequestParam(name = "scope", required = false) ReportScope scope,
            @RequestParam(name = "room_id", required = false) @Min(1) Long roomId) {
        return ApiResponse.success(reportService.rooms(resolveMonth(month), resolveScope(scope), roomId));
    }

    private YearMonth resolveMonth(String month) {
        return month == null ? YearMonth.now(BUSINESS_ZONE) : YearMonth.parse(month);
    }

    private ReportScope resolveScope(ReportScope scope) {
        return scope == null ? ReportScope.ALL : scope;
    }
}
