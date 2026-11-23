package com.qianxun.homestay.modules.auth;

import com.qianxun.homestay.modules.auth.domain.VerifiedWechatIdentity;
import com.qianxun.homestay.modules.auth.service.WechatIdentityService;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/** 为业务集成测试提供不访问微信服务的登录凭证。 */
public final class WechatLoginTestSupport {

    private WechatLoginTestSupport() {
    }

    /** 按测试请求中的 phone_code 返回对应手机号及独立 OpenID。 */
    public static void stub(WechatIdentityService identityService, String phone) {
        when(identityService.verifyLoginCredentials(anyString(), anyString()))
                .thenReturn(new VerifiedWechatIdentity(phone, "test-openid-" + phone));
    }

    /** 创建符合当前登录契约的测试请求体。 */
    public static String request(String phone) {
        return "{\"login_code\":\"test-login-code\",\"phone_code\":\"" + phone + "\"}";
    }
}
