package com.qianxun.homestay.modules.auth.vo;

/**
 * A01/A05 小程序会员登录成功响应数据。
 */
public record AppLoginVO(
        /** 会员 JWT。 */
        String token,
        /** 会员 JWT 有效秒数，V3.1 固定为 30 天。 */
        Long expiresIn,
        /** 当前会员资料。 */
        MemberVO member) {
}
