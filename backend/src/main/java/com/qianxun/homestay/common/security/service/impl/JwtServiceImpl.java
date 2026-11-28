package com.qianxun.homestay.common.security.service.impl;

import com.qianxun.homestay.common.security.AuthPrincipal;
import com.qianxun.homestay.common.security.UserRole;
import com.qianxun.homestay.common.security.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** 签发和校验会员、管理员分离的 JWT；密钥由部署环境提供。 */
@Service
public class JwtServiceImpl implements JwtService {

    private static final int MEMBER_AUTH_VERSION = 2;
    private static final int MIN_SIGNING_KEY_BYTES = 32;
    private static final String JWT_SECRET_ERROR = "请设置 JWT_SECRET，使用 Base64 编码的至少 32 字节随机密钥";

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
        this.signingKey = loadSigningKey(base64Secret);
        this.memberExpirationSeconds = memberExpirationSeconds;
        this.adminExpirationSeconds = adminExpirationSeconds;
    }

    /** 加载运行环境密钥，配置错误时在启动日志中提示配置项名称。 */
    private static SecretKey loadSigningKey(String base64Secret) {
        byte[] secretBytes;
        try {
            secretBytes = Decoders.BASE64.decode(base64Secret);
        } catch (DecodingException exception) {
            throw new IllegalStateException(JWT_SECRET_ERROR);
        }
        if (secretBytes.length < MIN_SIGNING_KEY_BYTES) {
            throw new IllegalStateException(JWT_SECRET_ERROR);
        }
        return Keys.hmacShaKeyFor(secretBytes);
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
        // 强制会员重新完成微信手机号授权，管理员令牌沿用独立认证流程。
        if (role == UserRole.MEMBER && !Integer.valueOf(MEMBER_AUTH_VERSION)
                .equals(claims.get("auth_version", Integer.class))) {
            throw new JwtException("Unsupported member authentication version");
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
            builder.claim("auth_version", MEMBER_AUTH_VERSION);
        }
        return builder.signWith(signingKey).compact();
    }
}
