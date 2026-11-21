package com.qianxun.homestay.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 小程序微信身份与手机号授权登录请求。
 */
public class PhoneLoginRequest {

    /** 微信登录一次性凭证。 */
    @NotBlank @Size(max = 128)
    private String loginCode;
    /** 微信手机号授权一次性凭证。 */
    @NotBlank @Size(max = 128)
    private String phoneCode;

    public String getLoginCode() { return loginCode; }
    public void setLoginCode(String loginCode) { this.loginCode = loginCode; }
    public String getPhoneCode() { return phoneCode; }
    public void setPhoneCode(String phoneCode) { this.phoneCode = phoneCode; }
}
