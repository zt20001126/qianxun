package com.qianxun.homestay.modules.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 微信支付前用于绑定真实付款人身份的一次性授权凭证。 */
public record WechatPrepayRequest(
        /** wx.login 返回的一次性登录 code。 */
        @NotBlank @Size(max = 128) String loginCode) {
}
