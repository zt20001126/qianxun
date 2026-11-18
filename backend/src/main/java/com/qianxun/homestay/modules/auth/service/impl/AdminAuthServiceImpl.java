package com.qianxun.homestay.modules.auth.service.impl;

import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.common.security.JwtService;
import com.qianxun.homestay.modules.admin.entity.AdminEntity;
import com.qianxun.homestay.modules.admin.mapper.AdminMapper;
import com.qianxun.homestay.modules.auth.service.AdminAuthService;
import com.qianxun.homestay.modules.auth.vo.AdminLoginVO;
import com.qianxun.homestay.modules.auth.vo.AdminVO;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 管理员认证编排：查询启用账号、BCrypt 校验、签发管理员角色令牌。
 */
@Service
public class AdminAuthServiceImpl implements AdminAuthService {

    private final AdminMapper adminMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AdminAuthServiceImpl(AdminMapper adminMapper, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.adminMapper = adminMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * 管理员登录流程。
     *
     * <p>Step 1: 按用户名读取账号，不在错误信息中回显密码或哈希。
     * Step 2: 校验账号启用状态和 BCrypt 密码。
     * Step 3: 签发带 ADMIN 角色及管理员 ID 的独立 JWT。</p>
     */
    @Override
    @Transactional(readOnly = true)
    public AdminLoginVO login(String username, String password) {
        AdminEntity admin = adminMapper.findByUsername(username);
        if (admin == null || !Boolean.TRUE.equals(admin.getEnabled())
                || !passwordEncoder.matches(password, admin.getPasswordHash())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }
        return new AdminLoginVO(jwtService.issueAdminToken(admin.getId()), toAdminVO(admin));
    }

    /**
     * 管理员当前身份读取流程。
     *
     * <p>Step 1: 仅按 JWT 管理员 ID 查询账号。
     * Step 2: 账号不存在或已禁用时拒绝访问。</p>
     */
    @Override
    @Transactional(readOnly = true)
    public AdminVO getCurrentAdmin(Long adminId) {
        AdminEntity admin = adminMapper.findById(adminId);
        if (admin == null || !Boolean.TRUE.equals(admin.getEnabled())) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "管理员账号不存在");
        }
        return toAdminVO(admin);
    }

    private AdminVO toAdminVO(AdminEntity admin) {
        return new AdminVO(admin.getId(), admin.getUsername());
    }
}
