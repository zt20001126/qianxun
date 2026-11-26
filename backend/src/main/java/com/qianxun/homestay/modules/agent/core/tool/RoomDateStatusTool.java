package com.qianxun.homestay.modules.agent.core.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.modules.report.service.ReportService;
import com.qianxun.homestay.modules.report.vo.RoomDateStatusVO;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** 校验模型输入并查询指定日期的后台房态和预订摘要。 */
@Component
public class RoomDateStatusTool implements AgentTool {
    private static final Set<String> ALLOWED_FIELDS = Set.of("date", "room_number");
    private final ReportService reportService;
    private final ObjectMapper mapper;

    public RoomDateStatusTool(ReportService reportService, ObjectMapper mapper) {
        this.reportService = reportService;
        this.mapper = mapper;
    }

    @Override
    public String name() { return "get_room_status_by_date"; }

    @Override
    public String description() { return "查询指定住宿日期单个或全部房间的后台房态及正式订单预订人和入住退房日期"; }

    @Override
    public Map<String, Object> parameters() {
        return Map.of("type", "object", "properties", Map.of(
                "date", Map.of("type", "string", "description", "北京时间住宿日期，格式 YYYY-MM-DD"),
                "room_number", Map.of("type", "string", "description", "可选房间号；省略则查询全部")),
                "required", List.of("date"), "additionalProperties", false);
    }

    @Override
    public AgentToolResult invoke(JsonNode arguments) {
        if (arguments == null || !arguments.isObject()
                || !arguments.has("date") || !arguments.get("date").isTextual()
                || arguments.properties().stream().anyMatch(entry -> !ALLOWED_FIELDS.contains(entry.getKey()))
                || (arguments.has("room_number") && (!arguments.get("room_number").isTextual()
                || arguments.get("room_number").asText().isBlank()))) {
            throw invalidParameters();
        }
        String dateText = arguments.get("date").asText();
        LocalDate date;
        try {
            if (!dateText.matches("\\d{4}-\\d{2}-\\d{2}")) throw invalidParameters();
            date = LocalDate.parse(dateText);
            if (date.getYear() < 1) throw invalidParameters();
        } catch (DateTimeParseException e) {
            throw invalidParameters();
        }
        String roomNumber = arguments.has("room_number") ? arguments.get("room_number").asText().strip() : null;
        List<RoomDateStatusVO> rooms = reportService.roomDateStatuses(date).stream()
                .filter(room -> roomNumber == null || roomNumber.equals(room.roomNumber())).toList();
        // 未找到房间必须明确告知模型，不能把空结果解释为空房。
        Map<String, Object> result = roomNumber != null && rooms.isEmpty()
                ? Map.of("date", dateText, "room_number", roomNumber, "room_found", false, "rooms", rooms)
                : Map.of("date", dateText, "room_found", true, "rooms", rooms);
        try {
            return AgentToolResult.completed(mapper.writeValueAsString(result));
        } catch (JsonProcessingException e) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "房态数据处理失败");
        }
    }

    private BusinessException invalidParameters() {
        return new BusinessException(HttpStatus.BAD_REQUEST, "房态查询参数无效");
    }
}
