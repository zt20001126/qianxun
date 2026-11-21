package com.qianxun.homestay.modules.auth.service;

/** 验证微信登录凭证与手机号授权，并安全绑定会员 OpenID。 */
public interface WechatIdentityService {

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
