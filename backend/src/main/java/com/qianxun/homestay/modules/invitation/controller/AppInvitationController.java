package com.qianxun.homestay.modules.invitation.controller;

import com.qianxun.homestay.common.api.ApiResponse;
import com.qianxun.homestay.common.security.AuthPrincipal;
import com.qianxun.homestay.modules.invitation.dto.InvitationBindDTO;
import com.qianxun.homestay.modules.invitation.service.InvitationService;
import com.qianxun.homestay.modules.invitation.vo.InvitationCodeVO;
import com.qianxun.homestay.modules.invitation.vo.InvitationPageVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/** 小程序会员邀请分享、绑定与本人记录接口。 */
@RestController
@Validated
@RequestMapping("/api/app/invitations")
public class AppInvitationController {
    private final InvitationService service;
    public AppInvitationController(InvitationService service) { this.service = service; }

    /** 获取当前会员的分享标识。 */
    @PostMapping("/share-code")
    public ApiResponse<InvitationCodeVO> code(@AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.success(new InvitationCodeVO(service.getOrCreateCode(principal.id())));
    }

    /** 静默处理绑定请求，不泄露已有关系。 */
    @PostMapping("/bind")
    public ApiResponse<java.util.Map<String, Object>> bind(@AuthenticationPrincipal AuthPrincipal principal,
                                                            @Valid @RequestBody InvitationBindDTO request) {
        service.bind(principal.id(), request.inviteCode());
        return ApiResponse.success(java.util.Map.of());
    }

    /** 查询当前会员邀请的用户及人数。 */
    @GetMapping
    public ApiResponse<InvitationPageVO> list(@AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(service.list(principal.id(), page, size));
    }
}
