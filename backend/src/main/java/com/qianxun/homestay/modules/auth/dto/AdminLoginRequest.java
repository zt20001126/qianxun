package com.qianxun.homestay.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 管理员用户名和密码登录请求，密码只用于校验，不写入日志或响应。
 */
public class AdminLoginRequest {

    /** 后台管理员登录用户名。 */
    @NotBlank(message = "请输入用户名")
    @Size(max = 60, message = "用户名长度不能超过 60 个字符")
    private String username;
    /** 管理员明文输入密码，仅在服务端与 BCrypt 哈希比较。 */
    @NotBlank(message = "请输入密码")
    @Size(max = 128, message = "密码长度不能超过 128 个字符")
    private String password;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
