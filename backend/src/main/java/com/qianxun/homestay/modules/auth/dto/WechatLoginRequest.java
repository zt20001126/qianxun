package com.qianxun.homestay.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** 小程序微信手机号授权登录请求。 */
public class WechatLoginRequest {

    /** wx.login 返回的一次性登录凭证。 */
    @NotBlank(message = "微信登录凭证不能为空")
    private String loginCode;

    /** getPhoneNumber 事件返回的一次性手机号授权凭证。 */
    @NotBlank(message = "微信手机号授权凭证不能为空")
    private String phoneCode;

    public String getLoginCode() { return loginCode; }
    public void setLoginCode(String loginCode) { this.loginCode = loginCode; }
    public String getPhoneCode() { return phoneCode; }
    public void setPhoneCode(String phoneCode) { this.phoneCode = phoneCode; }
}
