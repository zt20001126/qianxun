package com.qianxun.homestay.modules.auth.controller;

import com.qianxun.homestay.common.api.ApiResponse;
import com.qianxun.homestay.common.security.AuthPrincipal;
import com.qianxun.homestay.modules.auth.dto.WechatLoginRequest;
import com.qianxun.homestay.modules.auth.service.AuthService;
import com.qianxun.homestay.modules.auth.vo.AppLoginVO;
import com.qianxun.homestay.modules.auth.vo.MemberVO;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A01/A02 小程序会员认证接口，仅负责参数绑定和响应封装。
 */
@RestController
@RequestMapping("/api/app/auth")
public class AppAuthController {

    private final AuthService authService;

    public AppAuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * A01：微信手机号授权登录并在首次登录时自动创建会员。
     *
     * @param request 微信登录及手机号授权凭证
     * @return JWT 和会员资料
     */
    @PostMapping("/login")
    public ApiResponse<AppLoginVO> login(@Valid @RequestBody WechatLoginRequest request) {
        return ApiResponse.success(authService.loginWithWechat(request.getLoginCode(), request.getPhoneCode()));
    }

    /**
     * A02：返回当前会员资料。
     *
     * @param principal JWT 身份
     * @return 当前会员资料
     */
    @GetMapping("/me")
    public ApiResponse<MemberVO> me(@AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.success(authService.getCurrentMember(principal.id()));
    }
}
