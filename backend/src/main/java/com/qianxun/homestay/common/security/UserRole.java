package com.qianxun.homestay.common.security;

/**
 * 当前阶段支持的两种互相隔离的令牌身份。
 */
public enum UserRole {
    /** 小程序会员身份，用于限制会员端资源访问范围。 */
    MEMBER,
    /** 后台管理员身份，用于限制管理端资源访问范围。 */
    ADMIN
}
