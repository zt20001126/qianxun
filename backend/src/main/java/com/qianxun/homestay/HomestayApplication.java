package com.qianxun.homestay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * 芊浔民宿后端启动入口，负责加载认证服务及其基础设施。
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class HomestayApplication {

    /**
     * 启动 Spring Boot 应用。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(HomestayApplication.class, args);
    }
}
