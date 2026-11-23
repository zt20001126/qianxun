package com.qianxun.homestay.modules.auth.service;

import com.qianxun.homestay.modules.auth.domain.VerifiedWechatIdentity;

/** 在事务中核对已由微信验证的手机号并绑定会员 OpenID。 */
public interface WechatIdentityBindingService {

    /**
     * 按已验证身份创建或复用会员并安全绑定 OpenID。
     *
     * @param identity 微信服务端验证后的手机号和 OpenID
     * @param defaultAvatar 新建会员使用的默认头像
     * @return 已绑定会员 ID
     */
    Long loginOrBindVerifiedIdentity(VerifiedWechatIdentity identity, String defaultAvatar);

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
