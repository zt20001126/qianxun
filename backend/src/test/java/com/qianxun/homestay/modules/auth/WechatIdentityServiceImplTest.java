package com.qianxun.homestay.modules.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.modules.auth.service.WechatIdentityBindingService;
import com.qianxun.homestay.modules.auth.service.impl.WechatIdentityServiceImpl;
import com.qianxun.homestay.modules.payment.config.WechatPayProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** 使用本地 HTTP stub 验证微信 code2session、access_token 与手机号授权流程。 */
@ExtendWith(OutputCaptureExtension.class)
class WechatIdentityServiceImplTest {

    private static final Long MEMBER_ID = 22L;
    private static final String APP_ID = "wx-test-app";
    private static final String APP_SECRET = "test-secret";
    private static final String OPENID = "wx-openid-022";
    private static final String PHONE = "13800138000";

    private final WechatPayProperties properties = new WechatPayProperties();
    private final WechatIdentityBindingService bindingService = mock(WechatIdentityBindingService.class);
    private final RestClient.Builder restClientBuilder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
    private final WechatIdentityServiceImpl service = new WechatIdentityServiceImpl(
            restClientBuilder, new ObjectMapper(), properties, bindingService);

    @BeforeEach
    void enableWechatIdentity() {
        properties.setEnabled(true);
        properties.setAppId(APP_ID);
        properties.setAppSecret(APP_SECRET);
        when(bindingService.bindVerifiedIdentity(MEMBER_ID, PHONE, OPENID)).thenReturn(OPENID);
    }

    /** 微信可能将有效 JSON 标记为 text/plain，三步兑换均需兼容。 */
    @ParameterizedTest
    @ValueSource(strings = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE})
    void obtainsOpenIdAndAuthorizedPhoneThenBindsThemToCurrentMember(String contentType) {
        expectIdentityLookup("login-code", "phone-code", MediaType.parseMediaType(contentType));

        assertThat(service.verifyAndBindOpenId(MEMBER_ID, "login-code", "phone-code")).isEqualTo(OPENID);

        verify(bindingService).bindVerifiedIdentity(MEMBER_ID, PHONE, OPENID);
        server.verify();
    }

    @Test
    void loginIdentityCanBeVerifiedWhenWechatPaymentIsDisabled() {
        properties.setEnabled(false);
        expectIdentityLookup("login-code", "phone-code", MediaType.APPLICATION_JSON);

        assertThat(service.verifyLoginCredentials("login-code", "phone-code"))
                .isEqualTo(new com.qianxun.homestay.modules.auth.domain.VerifiedWechatIdentity(PHONE, OPENID));

        server.verify();
    }

    @Test
    void rejectsWechatLoginErrorWithoutRequestingPhoneOrBindingIdentity(CapturedOutput output) {
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/sns/jscode2session")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"errcode\":40029,\"errmsg\":\"invalid code\"}",
                        MediaType.TEXT_PLAIN));

        assertThatThrownBy(() -> service.verifyAndBindOpenId(MEMBER_ID, "bad-code", "phone-code"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("微信身份验证失败，请重试");

        assertThat(output.getAll()).contains("step=code2session errcode=40029")
                .doesNotContain("invalid code", APP_SECRET, "bad-code", "phone-code");

        verify(bindingService, org.mockito.Mockito.never()).bindVerifiedIdentity(MEMBER_ID, PHONE, OPENID);
        server.verify();
    }

    @Test
    void rejectsIncompleteSessionResponse() {
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/sns/jscode2session")))
                .andRespond(withSuccess("{\"session_key\":\"no-openid\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.verifyAndBindOpenId(MEMBER_ID, "login-code", "phone-code"))
                .isInstanceOf(BusinessException.class);

        verify(bindingService, org.mockito.Mockito.never()).bindVerifiedIdentity(MEMBER_ID, PHONE, OPENID);
        server.verify();
    }

    @Test
    void logsWechatAccessTokenErrorWithoutRequestingPhone(CapturedOutput output) {
        expectSession("login-code", MediaType.APPLICATION_JSON);
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/cgi-bin/token")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"errcode\":40125,\"errmsg\":\"invalid app secret\"}", MediaType.TEXT_PLAIN));

        assertThatThrownBy(() -> service.verifyAndBindOpenId(MEMBER_ID, "login-code", "phone-code"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("微信身份验证失败，请重试");

        assertThat(output.getAll()).contains("step=access_token errcode=40125")
                .doesNotContain("invalid app secret", APP_SECRET, OPENID);
        verify(bindingService, org.mockito.Mockito.never()).bindVerifiedIdentity(MEMBER_ID, PHONE, OPENID);
        server.verify();
    }

    @Test
    void rejectsPhoneAuthorizationFailureWithoutBindingIdentity(CapturedOutput output) {
        expectSession("login-code", MediaType.APPLICATION_JSON);
        expectAccessToken(MediaType.APPLICATION_JSON);
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/wxa/business/getuserphonenumber")))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"code\":\"bad-phone-code\"}"))
                .andRespond(withSuccess("{\"errcode\":40013,\"errmsg\":\"invalid phone code\"}",
                        MediaType.TEXT_PLAIN));

        assertThatThrownBy(() -> service.verifyAndBindOpenId(MEMBER_ID, "login-code", "bad-phone-code"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("微信身份验证失败，请重试");

        assertThat(output.getAll()).contains("step=getuserphonenumber errcode=40013")
                .doesNotContain("invalid phone code", "bad-phone-code", "access-token", OPENID, PHONE);

        verify(bindingService, org.mockito.Mockito.never()).bindVerifiedIdentity(MEMBER_ID, PHONE, OPENID);
        server.verify();
    }

    @Test
    void cachesAccessTokenAcrossPaymentAttempts() {
        expectIdentityLookup("login-code-1", "phone-code-1", MediaType.APPLICATION_JSON);
        expectSession("login-code-2", MediaType.APPLICATION_JSON);
        expectPhoneAuthorization("phone-code-2", MediaType.APPLICATION_JSON);

        service.verifyAndBindOpenId(MEMBER_ID, "login-code-1", "phone-code-1");
        service.verifyAndBindOpenId(MEMBER_ID, "login-code-2", "phone-code-2");

        verify(bindingService, times(2)).bindVerifiedIdentity(MEMBER_ID, PHONE, OPENID);
        server.verify();
    }

    @Test
    void mapsWechatNetworkFailureToGatewayError() {
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/sns/jscode2session")))
                .andRespond(withServerError());

        assertThatThrownBy(() -> service.verifyAndBindOpenId(MEMBER_ID, "login-code", "phone-code"))
                .isInstanceOf(BusinessException.class);

        verify(bindingService, org.mockito.Mockito.never()).bindVerifiedIdentity(MEMBER_ID, PHONE, OPENID);
        server.verify();
    }

    private void expectIdentityLookup(String loginCode, String phoneCode, MediaType contentType) {
        expectSession(loginCode, contentType);
        expectAccessToken(contentType);
        expectPhoneAuthorization(phoneCode, contentType);
    }

    private void expectSession(String loginCode, MediaType contentType) {
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/sns/jscode2session")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("appid", APP_ID))
                .andExpect(queryParam("secret", APP_SECRET))
                .andExpect(queryParam("js_code", loginCode))
                .andRespond(withSuccess("{\"openid\":\"" + OPENID + "\",\"session_key\":\"session\"}",
                        contentType));
    }

    private void expectAccessToken(MediaType contentType) {
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/cgi-bin/token")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("appid", APP_ID))
                .andExpect(queryParam("secret", APP_SECRET))
                .andRespond(withSuccess("{\"access_token\":\"access-token\",\"expires_in\":7200}",
                        contentType));
    }

    private void expectPhoneAuthorization(String phoneCode, MediaType contentType) {
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/wxa/business/getuserphonenumber")))
                .andExpect(method(HttpMethod.POST))
                .andExpect(queryParam("access_token", "access-token"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(request -> assertThat(request.getHeaders().getContentLength())
                        .isEqualTo(((MockClientHttpRequest) request).getBodyAsBytes().length))
                .andExpect(content().json("{\"code\":\"" + phoneCode + "\"}"))
                .andRespond(withSuccess(
                        "{\"errcode\":0,\"phone_info\":{\"purePhoneNumber\":\"" + PHONE + "\"}}",
                        contentType));
    }
}
