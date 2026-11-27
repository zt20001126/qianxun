package com.qianxun.homestay.modules.auth.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.modules.auth.domain.VerifiedWechatIdentity;
import com.qianxun.homestay.modules.auth.service.WechatIdentityBindingService;
import com.qianxun.homestay.modules.auth.service.WechatIdentityService;
import com.qianxun.homestay.modules.payment.config.WechatPayProperties;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** 通过微信服务端接口换取 OpenID 和授权手机号，不向小程序下发服务端凭证。 */
@Service
public class WechatIdentityServiceImpl implements WechatIdentityService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WechatIdentityServiceImpl.class);
    private static final String WECHAT_HOST = "api.weixin.qq.com";
    private static final String CODE_SESSION_STEP = "code2session";
    private static final String ACCESS_TOKEN_STEP = "access_token";
    private static final String PHONE_NUMBER_STEP = "getuserphonenumber";
    private static final Pattern PHONE_DIGITS = Pattern.compile("\\D");

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final WechatPayProperties properties;
    private final WechatIdentityBindingService bindingService;
    private volatile CachedAccessToken cachedAccessToken;

    public WechatIdentityServiceImpl(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            WechatPayProperties properties,
            WechatIdentityBindingService bindingService) {
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.bindingService = bindingService;
    }

    @Override
    public VerifiedWechatIdentity verifyLoginCredentials(String loginCode, String phoneCode) {
        if (properties.getAppId() == null || properties.getAppId().isBlank()
                || properties.getAppSecret() == null || properties.getAppSecret().isBlank()) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "微信登录服务尚未配置");
        }
        JsonNode session = readWechatResponse(CODE_SESSION_STEP, restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https").host(WECHAT_HOST).path("/sns/jscode2session")
                        .queryParam("appid", properties.getAppId())
                        .queryParam("secret", properties.getAppSecret())
                        .queryParam("js_code", loginCode)
                        .queryParam("grant_type", "authorization_code")
                        .build()));
        String openid = requiredText(session, "openid");
        String token = accessToken();

        // 微信手机号接口拒绝分块请求，按 UTF-8 正文的实际字节数发送。
        byte[] phoneRequest = objectMapper.createObjectNode().put("code", phoneCode)
                .toString().getBytes(StandardCharsets.UTF_8);
        JsonNode phoneResponse = readWechatResponse(PHONE_NUMBER_STEP, restClient.post()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https").host(WECHAT_HOST).path("/wxa/business/getuserphonenumber")
                        .queryParam("access_token", token)
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .contentLength(phoneRequest.length)
                .body(phoneRequest));
        String phone = requiredText(phoneResponse.path("phone_info"), "purePhoneNumber");
        phone = PHONE_DIGITS.matcher(phone).replaceAll("");
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "微信返回的手机号格式无效");
        }
        return new VerifiedWechatIdentity(phone, openid);
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
            JsonNode response = readWechatResponse(ACCESS_TOKEN_STEP, restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https").host(WECHAT_HOST).path("/cgi-bin/token")
                            .queryParam("grant_type", "client_credential")
                            .queryParam("appid", properties.getAppId())
                            .queryParam("secret", properties.getAppSecret())
                            .build()));
            String token = requiredText(response, "access_token");
            long expiresIn = response.path("expires_in").asLong(7200);
            cachedAccessToken = new CachedAccessToken(token, Instant.now().plusSeconds(expiresIn));
            return token;
        }
    }

    /** 微信可能以 text/plain 返回 JSON，统一在这里解析并记录接口步骤。 */
    private JsonNode readWechatResponse(String step, RestClient.RequestHeadersSpec<?> request) {
        JsonNode response;
        try {
            byte[] body = request.retrieve().body(byte[].class);
            response = body == null ? null : objectMapper.readTree(body);
        } catch (RestClientException | IOException exception) {
            // 异常消息可能包含带凭证的请求地址或响应片段，仅记录异常类型。
            LOGGER.warn("微信身份接口调用或解析失败 step={} exception={}", step, exception.getClass().getSimpleName());
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "暂时无法验证微信身份，请稍后重试");
        }
        if (response == null || !response.isObject()) {
            LOGGER.warn("微信身份接口响应无有效 JSON 对象 step={}", step);
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "微信身份验证失败，请重试");
        }
        int errorCode = response.path("errcode").asInt();
        if (errorCode != 0) {
            LOGGER.warn("微信身份接口返回错误 step={} errcode={}", step, errorCode);
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "微信身份验证失败，请重试");
        }
        return response;
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
