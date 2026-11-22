package com.qianxun.homestay.modules.notification.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** 注册邮件通知配置属性，供通知模块读取环境变量中的收件人和发件人显示名。 */
@Configuration
@EnableConfigurationProperties(MailNotificationProperties.class)
public class MailNotificationConfiguration { }
