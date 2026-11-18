package com.qianxun.homestay.config;

import com.qianxun.homestay.modules.admin.entity.AdminEntity;
import com.qianxun.homestay.modules.admin.mapper.AdminMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 仅在 admin 表为空且部署环境显式提供凭据时创建首个管理员，不保存或记录明文密码。
 */
@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final AdminMapper adminMapper;
    private final PasswordEncoder passwordEncoder;
    private final String bootstrapUsername;
    private final String bootstrapPassword;

    public AdminBootstrapRunner(
            AdminMapper adminMapper,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.bootstrap-username:}") String bootstrapUsername,
            @Value("${app.admin.bootstrap-password:}") String bootstrapPassword) {
        this.adminMapper = adminMapper;
        this.passwordEncoder = passwordEncoder;
        this.bootstrapUsername = bootstrapUsername;
        this.bootstrapPassword = bootstrapPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (bootstrapUsername.isBlank() || bootstrapPassword.isBlank()) {
            LOGGER.info("管理员初始化凭据未设置；现有账号可正常登录");
            return;
        }
        if (adminMapper.countAdmins() > 0) {
            LOGGER.info("管理员账号已存在，跳过首次初始化");
            return;
        }

        AdminEntity admin = new AdminEntity();
        admin.setUsername(bootstrapUsername);
        admin.setPasswordHash(passwordEncoder.encode(bootstrapPassword));
        adminMapper.insert(admin);
        LOGGER.info("已通过环境配置初始化首个管理员账号");
    }
}
