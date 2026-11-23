package com.qianxun.homestay.modules.auth.domain;

/** 微信服务端验证后的会员手机号和小程序 OpenID，仅在后端业务中传递。 */
public record VerifiedWechatIdentity(String phone, String openid) {
}
