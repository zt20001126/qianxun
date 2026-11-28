package com.qianxun.homestay.common.logging;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** 验证登录手机号在请求日志中脱敏，包括无法解析的 JSON。 */
class ApiRequestLoggingFilterTest {

    @ParameterizedTest
    @ValueSource(strings = {"{\"phone\":\"13800000000\"}", "{\"phone\":\"13800000000"})
    void redactsPhoneLoginNumber(String body) throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(ApiRequestLoggingFilter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/app/auth/phone-login");
            request.setContentType("application/json");
            request.setContent(body.getBytes(StandardCharsets.UTF_8));
            new ApiRequestLoggingFilter(new ObjectMapper()).doFilter(request, new MockHttpServletResponse(),
                    (req, res) -> req.getInputStream().readAllBytes());
            assertThat(appender.list.getFirst().getFormattedMessage())
                    .contains("[REDACTED]").doesNotContain("13800000000");
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }
}
