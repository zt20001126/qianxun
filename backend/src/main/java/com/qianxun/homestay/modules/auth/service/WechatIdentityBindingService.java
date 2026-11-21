package com.qianxun.homestay.modules.auth.service;

/** 在事务中核对已由微信验证的手机号并绑定会员 OpenID。 */
public interface WechatIdentityBindingService {

    /**
     * 校验微信授权手机号属于当前会员，并绑定其 OpenID。
     *
     * @param memberId 当前 JWT 会员 ID
     * @param phone 微信返回的纯手机号
     * @param openid 当前小程序 OpenID
     * @return 绑定后的 OpenID
     */
    String bindVerifiedIdentity(Long memberId, String phone, String openid);
}
