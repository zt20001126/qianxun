package com.qianxun.homestay.modules.agent;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.modules.agent.core.tool.TodayBusinessDataTool;
import com.qianxun.homestay.modules.report.service.ReportService;
import com.qianxun.homestay.modules.report.vo.TodayBusinessDataVO;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
/** 今日查询工具仅接受空参数并调用报表业务接口。 */
class TodayBusinessDataToolTest {
    @Test void queriesShanghaiDateAndRejectsExtraParameters() throws Exception {
        var service = mock(ReportService.class);
        var json = new ObjectMapper().findAndRegisterModules();
        var date = LocalDate.now(ZoneId.of("Asia/Shanghai"));
        when(service.dailyBusinessData(date)).thenReturn(new TodayBusinessDataVO(date, 1, 0, 1, "0.0%",
                "0.00", "0.00", "0.00", "0.00", "0.00"));
        var tool = new TodayBusinessDataTool(service, json);
        assertTrue(tool.invoke(json.readTree("{}")).content().contains("0.00"));
        verify(service).dailyBusinessData(date);
        assertThrows(BusinessException.class, () -> tool.invoke(json.readTree("{\"date\":\"2020-01-01\"}")));
    }
}
