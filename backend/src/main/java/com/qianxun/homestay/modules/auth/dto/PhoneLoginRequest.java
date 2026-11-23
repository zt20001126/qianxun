package com.qianxun.homestay.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** 小程序直接输入手机号登录请求；手机号归属不会经过验证。 */
public class PhoneLoginRequest {

    /** 中国大陆手机号字符串。 */
    @NotBlank(message = "请输入手机号")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入有效的中国大陆手机号")
    private String phone;

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
