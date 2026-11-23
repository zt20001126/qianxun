package com.qianxun.homestay.modules.auth.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.modules.auth.domain.VerifiedWechatIdentity;
import com.qianxun.homestay.modules.auth.service.WechatIdentityBindingService;
import com.qianxun.homestay.modules.auth.service.WechatIdentityService;
import com.qianxun.homestay.modules.payment.config.WechatPayProperties;
import java.time.Instant;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** 通过微信服务端接口换取 OpenID 和授权手机号，不向小程序下发服务端凭证。 */
@Service
public class WechatIdentityServiceImpl implements WechatIdentityService {

    private static final String CODE_SESSION_URL = "https://api.weixin.qq.com/sns/jscode2session";
    private static final String ACCESS_TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token";
    private static final String PHONE_NUMBER_URL = "https://api.weixin.qq.com/wxa/business/getuserphonenumber";
    private static final Pattern PHONE_DIGITS = Pattern.compile("\\D");

    private final RestClient restClient;
    private final WechatPayProperties properties;
    private final WechatIdentityBindingService bindingService;
    private volatile CachedAccessToken cachedAccessToken;

    public WechatIdentityServiceImpl(
            RestClient.Builder restClientBuilder,
            WechatPayProperties properties,
            WechatIdentityBindingService bindingService) {
        this.restClient = restClientBuilder.build();
        this.properties = properties;
        this.bindingService = bindingService;
    }

    @Override
    public VerifiedWechatIdentity verifyLoginCredentials(String loginCode, String phoneCode) {
        if (properties.getAppId() == null || properties.getAppId().isBlank()
                || properties.getAppSecret() == null || properties.getAppSecret().isBlank()) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "微信登录服务尚未配置");
        }
        try {
            JsonNode session = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https").host("api.weixin.qq.com").path("/sns/jscode2session")
                            .queryParam("appid", properties.getAppId())
                            .queryParam("secret", properties.getAppSecret())
                            .queryParam("js_code", loginCode)
                            .queryParam("grant_type", "authorization_code")
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
            ensureWechatSuccess(session);
            String openid = requiredText(session, "openid");

            JsonNode phoneResponse = restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https").host("api.weixin.qq.com").path("/wxa/business/getuserphonenumber")
                            .queryParam("access_token", accessToken())
                            .build())
                    .body(Map.of("code", phoneCode))
                    .retrieve()
                    .body(JsonNode.class);
            ensureWechatSuccess(phoneResponse);
            String phone = requiredText(phoneResponse.path("phone_info"), "purePhoneNumber");
            phone = PHONE_DIGITS.matcher(phone).replaceAll("");
            if (!phone.matches("^1[3-9]\\d{9}$")) {
                throw new BusinessException(HttpStatus.BAD_GATEWAY, "微信返回的手机号格式无效");
            }
            return new VerifiedWechatIdentity(phone, openid);
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "暂时无法验证微信身份，请稍后重试");
        }
    }

    @Override
    public String verifyAndBindOpenId(Long memberId, String loginCode, String phoneCode) {
        VerifiedWechatIdentity identity = verifyLoginCredentials(loginCode, phoneCode);
        return bindingService.bindVerifiedIdentity(memberId, identity.phone(), identity.openid());
    }

    private String accessToken() {
        CachedAccessToken cached = cachedAccessToken;
        if (cached != null && cached.expiresAt().isAfter(Instant.now().plusSeconds(60))) {
            return cached.value();
        }
        synchronized (this) {
            cached = cachedAccessToken;
            if (cached != null && cached.expiresAt().isAfter(Instant.now().plusSeconds(60))) {
                return cached.value();
            }
            JsonNode response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https").host("api.weixin.qq.com").path("/cgi-bin/token")
                            .queryParam("grant_type", "client_credential")
                            .queryParam("appid", properties.getAppId())
                            .queryParam("secret", properties.getAppSecret())
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
            ensureWechatSuccess(response);
            String token = requiredText(response, "access_token");
            long expiresIn = response.path("expires_in").asLong(7200);
            cachedAccessToken = new CachedAccessToken(token, Instant.now().plusSeconds(expiresIn));
            return token;
        }
    }

    private void ensureWechatSuccess(JsonNode response) {
        if (response == null || response.hasNonNull("errcode") && response.path("errcode").asInt() != 0) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "微信身份验证失败，请重试");
        }
    }

    private String requiredText(JsonNode node, String field) {
        String value = node == null ? null : node.path(field).asText("");
        if (value.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "微信身份验证返回信息不完整");
        }
        return value;
    }

    private record CachedAccessToken(String value, Instant expiresAt) {
    }
}
