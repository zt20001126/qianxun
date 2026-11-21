package com.qianxun.homestay.modules.auth.service;

/** 在事务中核对已由微信验证的手机号并绑定会员 OpenID。 */
public interface WechatIdentityBindingService {

    /** 根据已验证手机号创建或复用会员并绑定微信身份。 */
    com.qianxun.homestay.modules.member.entity.MemberEntity bindLoginIdentity(String phone, String openid);

    /** 验证当前微信身份与会员已经绑定的 OpenID 一致。 */
    String requireBoundOpenId(Long memberId, String openid);

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
