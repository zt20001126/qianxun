package com.qianxun.homestay.common.util;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/** 将数据库业务时间按项目时区转换为带偏移量的 API 时间。 */
public final class ApiTime {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private ApiTime() {
    }

    public static OffsetDateTime fromShanghai(LocalDateTime value) {
        return value == null ? null : value.atZone(BUSINESS_ZONE).toOffsetDateTime();
    }
}
