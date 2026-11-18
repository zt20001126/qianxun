package com.qianxun.homestay.modules.auth.vo;

/**
 * A03/A04 返回的管理员公开资料，不包含 BCrypt 密码哈希。
 */
public record AdminVO(
        /** 管理员数据库 ID。 */
        Long id,
        /** 管理员登录用户名。 */
        String username) {
}
