package com.qianxun.homestay.modules.member.controller;

import com.qianxun.homestay.common.api.ApiResponse;
import com.qianxun.homestay.common.security.AuthPrincipal;
import com.qianxun.homestay.modules.member.service.MemberService;
import com.qianxun.homestay.modules.member.vo.AppMemberVO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** M01 小程序会员资料接口。 */
@RestController
@Validated
@RequestMapping("/api/app/members/me")
public class AppMemberController {

    private final MemberService memberService;

    public AppMemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    /** 查询当前登录会员的等级、积分余额与进度。 */
    @GetMapping
    public ApiResponse<AppMemberVO> me(@AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.success(memberService.getMyMember(principal.id()));
    }
}
