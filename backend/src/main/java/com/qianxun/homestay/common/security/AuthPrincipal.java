package com.qianxun.homestay.common.security;

/**
 * 已通过 JWT 验证的当前用户身份，仅包含数据库身份 ID 与角色。
 *
 * @param id 数据库身份主键
 * @param role 会员或管理员角色
 */
public record AuthPrincipal(Long id, UserRole role) {
}
