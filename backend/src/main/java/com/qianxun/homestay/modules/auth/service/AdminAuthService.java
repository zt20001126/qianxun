package com.qianxun.homestay.modules.auth.service;

import com.qianxun.homestay.modules.auth.vo.AdminLoginVO;
import com.qianxun.homestay.modules.auth.vo.AdminVO;

/**
 * PC 管理员认证业务入口。
 */
public interface AdminAuthService {

    /**
     * 使用 BCrypt 校验用户名密码并签发管理员 JWT。
     *
     * @param username 管理员用户名
     * @param password 管理员明文输入密码
     * @return 管理员令牌和公开资料
     */
    AdminLoginVO login(String username, String password);

    /**
     * 根据 JWT 身份读取当前管理员资料。
     *
     * @param adminId 管理员 ID
     * @return 管理员公开资料
     */
    AdminVO getCurrentAdmin(Long adminId);
}
