package com.qianxun.homestay.modules.auth.controller;

import com.qianxun.homestay.common.api.ApiResponse;
import com.qianxun.homestay.common.security.AuthPrincipal;
import com.qianxun.homestay.modules.auth.dto.AdminLoginRequest;
import com.qianxun.homestay.modules.auth.service.AdminAuthService;
import com.qianxun.homestay.modules.auth.vo.AdminLoginVO;
import com.qianxun.homestay.modules.auth.vo.AdminVO;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A03/A04 管理员认证接口，仅负责参数绑定和响应封装。
 */
@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    public AdminAuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    /**
     * A03：校验用户名密码并签发管理员令牌。
     *
     * @param request 管理员登录参数
     * @return 管理员 JWT 和公开资料
     */
    @PostMapping("/login")
    public ApiResponse<AdminLoginVO> login(@Valid @RequestBody AdminLoginRequest request) {
        return ApiResponse.success(adminAuthService.login(request.getUsername(), request.getPassword()));
    }

    /**
     * A04：返回当前管理员公开资料。
     *
     * @param principal JWT 身份
     * @return 当前管理员资料
     */
    @GetMapping("/me")
    public ApiResponse<AdminVO> me(@AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.success(adminAuthService.getCurrentAdmin(principal.id()));
    }
}
