package com.qianxun.homestay.common.security.service.impl;

import com.qianxun.homestay.common.security.AuthPrincipal;
import com.qianxun.homestay.common.security.UserRole;
import com.qianxun.homestay.common.security.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** 签发和校验会员、管理员分离的 JWT；密钥由部署环境提供。 */
@Service
public class JwtServiceImpl implements JwtService {

    private static final String MEMBER_AUTH_VERSION_CLAIM = "member_auth_version";
    private static final int MEMBER_AUTH_VERSION = 2;

    private final SecretKey signingKey;
    private final long memberExpirationSeconds;
    private final long adminExpirationSeconds;

    /**
     * 初始化签名密钥和两类令牌的有效时长。
     *
     * @param base64Secret Base64 编码的至少 256 位随机密钥
     * @param memberExpirationSeconds 会员令牌秒数
     * @param adminExpirationSeconds 管理员令牌秒数
     */
    public JwtServiceImpl(
            @Value("${app.jwt.secret}") String base64Secret,
            @Value("${app.jwt.member-expiration-seconds}") long memberExpirationSeconds,
            @Value("${app.jwt.admin-expiration-seconds}") long adminExpirationSeconds) {
        byte[] secretBytes = Decoders.BASE64.decode(base64Secret);
        this.signingKey = Keys.hmacShaKeyFor(secretBytes);
        this.memberExpirationSeconds = memberExpirationSeconds;
        this.adminExpirationSeconds = adminExpirationSeconds;
    }

    /**
     * 签发会员令牌。
     *
     * @param memberId 会员 ID
     * @return JWT 字符串
     */
    @Override
    public String issueMemberToken(Long memberId) {
        return issueToken(memberId, UserRole.MEMBER, memberExpirationSeconds);
    }

    /**
     * 签发管理员令牌。
     *
     * @param adminId 管理员 ID
     * @return JWT 字符串
     */
    @Override
    public String issueAdminToken(Long adminId) {
        return issueToken(adminId, UserRole.ADMIN, adminExpirationSeconds);
    }

    /**
     * 返回会员令牌有效秒数，用于 A01 契约。
     *
     * @return 有效秒数
     */
    @Override
    public long getMemberExpirationSeconds() {
        return memberExpirationSeconds;
    }

    /**
     * 校验 JWT 签名、时效、角色和身份主键。
     *
     * @param token Authorization Bearer 后的令牌
     * @return 当前认证身份
     * @throws JwtException 令牌无效或已过期
     */
    @Override
    public AuthPrincipal parse(String token) throws JwtException {
        Claims claims = Jwts.parser().verifyWith(signingKey).build()
                .parseSignedClaims(token).getPayload();
        UserRole role = UserRole.valueOf(claims.get("role", String.class));
        if (role == UserRole.MEMBER && !Integer.valueOf(MEMBER_AUTH_VERSION)
                .equals(claims.get(MEMBER_AUTH_VERSION_CLAIM, Integer.class))) {
            throw new JwtException("Member authentication version invalid");
        }
        String identityClaim = role == UserRole.MEMBER ? "member_id" : "admin_id";
        Object identityValue = claims.get(identityClaim);
        if (identityValue == null) {
            throw new JwtException("Missing identity claim");
        }
        return new AuthPrincipal(Long.valueOf(identityValue.toString()), role);
    }

    private String issueToken(Long identityId, UserRole role, long expirationSeconds) {
        Instant issuedAt = Instant.now();
        String identityClaim = role == UserRole.MEMBER ? "member_id" : "admin_id";
        var builder = Jwts.builder()
                .subject(identityId.toString())
                .claim(identityClaim, identityId)
                .claim("role", role.name())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plusSeconds(expirationSeconds)));
        if (role == UserRole.MEMBER) {
            builder.claim(MEMBER_AUTH_VERSION_CLAIM, MEMBER_AUTH_VERSION);
        }
        return builder.signWith(signingKey).compact();
    }
}
