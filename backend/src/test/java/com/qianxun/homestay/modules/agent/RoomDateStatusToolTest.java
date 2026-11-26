package com.qianxun.homestay.modules.agent;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.modules.agent.core.tool.RoomDateStatusTool;
import com.qianxun.homestay.modules.report.service.ReportService;
import com.qianxun.homestay.modules.report.vo.RoomDateStatusVO;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 指定日期房态工具的输入校验和最小数据输出测试。 */
class RoomDateStatusToolTest {
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    private final ReportService service = mock(ReportService.class);
    private final RoomDateStatusTool tool = new RoomDateStatusTool(service, json);

    @Test
    void filtersRoomAndReturnsOnlyRequiredFields() throws Exception {
        LocalDate date = LocalDate.of(2026, 11, 28);
        when(service.roomDateStatuses(date)).thenReturn(List.of(
                new RoomDateStatusVO("101", "OCCUPIED", "PENDING_CHECKIN", "张先生", date.minusDays(1), date.plusDays(1)),
                new RoomDateStatusVO("102", "AVAILABLE", null, null, null, null)));
        var result = json.readTree(tool.invoke(json.readTree("{\"date\":\"2026-11-28\",\"room_number\":\"101\"}")).content());
        assertEquals(1, result.path("rooms").size());
        assertEquals("张先生", result.path("rooms").get(0).path("guest_name").asText());
        assertEquals("PENDING_CHECKIN", result.path("rooms").get(0).path("order_status").asText());
        assertFalse(result.toString().contains("phone"));
        assertEquals(2, json.readTree(tool.invoke(json.readTree("{\"date\":\"2026-11-28\"}")).content()).path("rooms").size());
        assertFalse(json.readTree(tool.invoke(json.readTree("{\"date\":\"2026-11-28\",\"room_number\":\"999\"}")).content()).path("room_found").asBoolean());
    }

    @Test
    void rejectsInvalidArgumentsBeforeQuery() throws Exception {
        for (String input : List.of("{}", "[]", "{\"date\":\"2026-02-30\"}",
                "{\"date\":\"2026-1-01\"}", "{\"date\":null}",
                "{\"date\":\"2026-01-01\",\"room_number\":3}",
                "{\"date\":\"2026-01-01\",\"extra\":true}")) {
            assertThrows(BusinessException.class, () -> tool.invoke(json.readTree(input)), input);
        }
        verifyNoInteractions(service);
    }
}
