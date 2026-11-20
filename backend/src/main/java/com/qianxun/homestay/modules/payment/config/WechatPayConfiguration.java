package com.qianxun.homestay.modules.payment.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** 注册微信支付配置属性。 */
@Configuration
@EnableConfigurationProperties(WechatPayProperties.class)
public class WechatPayConfiguration {
}
