package com.qianxun.homestay.modules.auth.vo;

/**
 * A03 管理员登录成功响应数据。
 */
public record AdminLoginVO(
        /** 管理员 JWT。 */
        String token,
        /** 当前管理员公开资料。 */
        AdminVO admin) {
}
