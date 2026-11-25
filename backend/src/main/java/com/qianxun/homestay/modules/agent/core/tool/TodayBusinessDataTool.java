package com.qianxun.homestay.modules.agent.core.tool;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.modules.report.service.ReportService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
/** 查询北京时间今日的经营看板汇总，不返回订单或顾客明细。 */
@Component
public class TodayBusinessDataTool implements AgentTool {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private final ReportService reportService;
    private final ObjectMapper mapper;
    public TodayBusinessDataTool(ReportService reportService, ObjectMapper mapper) {
        this.reportService = reportService;
        this.mapper = mapper;
    }
    @Override public String name() { return "get_today_business_data"; }
    @Override public String description() { return "查询北京时间今日的房间统计、入住率、营业额、手续费、提成及商家到手金额"; }
    @Override public Map<String, Object> parameters() {
        return Map.of("type", "object", "properties", Map.of(), "additionalProperties", false);
    }
    @Override public AgentToolResult invoke(JsonNode arguments) {
        if (arguments == null || !arguments.isObject() || !arguments.isEmpty())
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "查询工具参数无效");
        try {
            return AgentToolResult.completed(mapper.writeValueAsString(reportService.dailyBusinessData(LocalDate.now(BUSINESS_ZONE))));
        } catch (JsonProcessingException e) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "经营数据处理失败");
        }
    }
}
