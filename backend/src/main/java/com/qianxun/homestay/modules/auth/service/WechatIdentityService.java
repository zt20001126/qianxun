package com.qianxun.homestay.modules.auth.service;

import com.qianxun.homestay.modules.auth.domain.VerifiedWechatIdentity;

/** 验证微信登录凭证与手机号授权，并安全绑定会员 OpenID。 */
public interface WechatIdentityService {

    /**
     * 在登录前向微信验证登录凭证与手机号授权凭证。
     *
     * @param loginCode wx.login 返回的一次性凭证
     * @param phoneCode wx.getPhoneNumber 返回的一次性凭证
     * @return 微信验证后的手机号和 OpenID
     */
    VerifiedWechatIdentity verifyLoginCredentials(String loginCode, String phoneCode);

    /**
     * 使用小程序登录 code 和手机号授权 code 验证当前会员后绑定 OpenID。
     *
     * @param memberId 当前 JWT 会员 ID
     * @param loginCode wx.login 返回的一次性凭证
     * @param phoneCode wx.getPhoneNumber 返回的一次性凭证
     * @return 当前小程序下的 OpenID
     */
    String verifyAndBindOpenId(Long memberId, String loginCode, String phoneCode);
}
