package com.qianxun.homestay.modules.mall;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qianxun.homestay.common.logging.ApiRequestLoggingFilter;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** 确保商城收货人资料不会出现在请求日志中。 */
class MallRequestLoggingTest {
    @Test
    void recipientFieldsAreRedacted() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(ApiRequestLoggingFilter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/app/mall/payments");
            request.setContentType("application/json");
            request.setContent("{\"recipient_name\":\"张三\",\"recipient_phone\":\"13800000000\",\"shipping_address\":\"测试街\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            new ApiRequestLoggingFilter(new ObjectMapper()).doFilter(request, new MockHttpServletResponse(),
                    (req, res) -> req.getInputStream().readAllBytes());
            String message = appender.list.get(0).getFormattedMessage();
            assertThat(message).contains("[REDACTED]").doesNotContain("张三", "13800000000", "测试街");
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }
}
