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

import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.modules.auth.service.WechatIdentityBindingService;
import com.qianxun.homestay.modules.auth.service.impl.WechatIdentityServiceImpl;
import com.qianxun.homestay.modules.payment.config.WechatPayProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** 使用本地 HTTP stub 验证微信 code2session、access_token 与手机号授权流程。 */
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
            restClientBuilder, properties, bindingService);

    @BeforeEach
    void enableWechatIdentity() {
        properties.setEnabled(true);
        properties.setAppId(APP_ID);
        properties.setAppSecret(APP_SECRET);
        when(bindingService.bindVerifiedIdentity(MEMBER_ID, PHONE, OPENID)).thenReturn(OPENID);
    }

    @Test
    void obtainsOpenIdAndAuthorizedPhoneThenBindsThemToCurrentMember() {
        expectIdentityLookup("login-code", "phone-code");

        assertThat(service.verifyAndBindOpenId(MEMBER_ID, "login-code", "phone-code")).isEqualTo(OPENID);

        verify(bindingService).bindVerifiedIdentity(MEMBER_ID, PHONE, OPENID);
        server.verify();
    }

    @Test
    void loginIdentityCanBeVerifiedWhenWechatPaymentIsDisabled() {
        properties.setEnabled(false);
        expectIdentityLookup("login-code", "phone-code");

        assertThat(service.verifyLoginCredentials("login-code", "phone-code"))
                .isEqualTo(new com.qianxun.homestay.modules.auth.domain.VerifiedWechatIdentity(PHONE, OPENID));

        server.verify();
    }

    @Test
    void rejectsWechatLoginErrorWithoutRequestingPhoneOrBindingIdentity() {
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/sns/jscode2session")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"errcode\":40029,\"errmsg\":\"invalid code\"}",
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.verifyAndBindOpenId(MEMBER_ID, "bad-code", "phone-code"))
                .isInstanceOf(BusinessException.class);

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
    void rejectsPhoneAuthorizationFailureWithoutBindingIdentity() {
        expectSession("login-code");
        expectAccessToken();
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/wxa/business/getuserphonenumber")))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"code\":\"bad-phone-code\"}"))
                .andRespond(withSuccess("{\"errcode\":40013,\"errmsg\":\"invalid phone code\"}",
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.verifyAndBindOpenId(MEMBER_ID, "login-code", "bad-phone-code"))
                .isInstanceOf(BusinessException.class);

        verify(bindingService, org.mockito.Mockito.never()).bindVerifiedIdentity(MEMBER_ID, PHONE, OPENID);
        server.verify();
    }

    @Test
    void cachesAccessTokenAcrossPaymentAttempts() {
        expectIdentityLookup("login-code-1", "phone-code-1");
        expectSession("login-code-2");
        expectPhoneAuthorization("phone-code-2");

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

    private void expectIdentityLookup(String loginCode, String phoneCode) {
        expectSession(loginCode);
        expectAccessToken();
        expectPhoneAuthorization(phoneCode);
    }

    private void expectSession(String loginCode) {
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/sns/jscode2session")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("appid", APP_ID))
                .andExpect(queryParam("secret", APP_SECRET))
                .andExpect(queryParam("js_code", loginCode))
                .andRespond(withSuccess("{\"openid\":\"" + OPENID + "\",\"session_key\":\"session\"}",
                        MediaType.APPLICATION_JSON));
    }

    private void expectAccessToken() {
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/cgi-bin/token")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("appid", APP_ID))
                .andExpect(queryParam("secret", APP_SECRET))
                .andRespond(withSuccess("{\"access_token\":\"access-token\",\"expires_in\":7200}",
                        MediaType.APPLICATION_JSON));
    }

    private void expectPhoneAuthorization(String phoneCode) {
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/wxa/business/getuserphonenumber")))
                .andExpect(method(HttpMethod.POST))
                .andExpect(queryParam("access_token", "access-token"))
                .andExpect(content().json("{\"code\":\"" + phoneCode + "\"}"))
                .andRespond(withSuccess(
                        "{\"errcode\":0,\"phone_info\":{\"purePhoneNumber\":\"" + PHONE + "\"}}",
                        MediaType.APPLICATION_JSON));
    }
}
