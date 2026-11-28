package com.qianxun.homestay.config;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/** 验证展示缓存时效及分页配置边界。 */
class DisplayCachePropertiesTest {
    @Test void rejectsInvalidConfiguration() {
        var properties = new DisplayCacheProperties();
        assertThat(properties.isValid()).isTrue();
        properties.setImageTtl(Duration.ofMinutes(56));
        assertThat(properties.isValid()).isFalse();
        properties.setImageTtl(Duration.ofMinutes(50));
        properties.setPageSize(101);
        assertThat(properties.isValid()).isFalse();
        properties.setPageSize(20);
        properties.setRoomTtl(Duration.ZERO);
        assertThat(properties.isValid()).isFalse();
    }
}
