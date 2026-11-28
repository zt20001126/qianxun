package com.qianxun.homestay.common.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.qianxun.homestay.common.security.service.impl.JwtServiceImpl;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** 验证错误的 JWT 签名密钥能在启动时得到明确的配置提示。 */
class JwtServiceImplTest {

    @ParameterizedTest
    @ValueSource(strings = {"", "YQ==", "YWJ!ZA=="})
    void rejectsInvalidSigningKeyWithConfigurationName(String secret) {
        assertThatThrownBy(() -> new JwtServiceImpl(secret, 3600, 3600))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }
}
