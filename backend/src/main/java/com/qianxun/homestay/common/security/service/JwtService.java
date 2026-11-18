package com.qianxun.homestay.common.security.service;

import com.qianxun.homestay.common.security.AuthPrincipal;
import io.jsonwebtoken.JwtException;

/** 负责会员和管理员 JWT 签发与校验的安全服务接口。 */
public interface JwtService {

    /** 签发会员令牌。 */
    String issueMemberToken(Long memberId);

    /** 签发管理员令牌。 */
    String issueAdminToken(Long adminId);

    /** 返回会员令牌有效秒数。 */
    long getMemberExpirationSeconds();

    /** 校验令牌签名、时效、角色和身份主键。 */
    AuthPrincipal parse(String token) throws JwtException;
}
