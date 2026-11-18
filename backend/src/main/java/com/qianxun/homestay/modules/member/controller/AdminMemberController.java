package com.qianxun.homestay.modules.member.controller;

import com.qianxun.homestay.common.api.ApiResponse;
import com.qianxun.homestay.modules.member.dto.MemberCreateRequest;
import com.qianxun.homestay.modules.member.service.MemberService;
import com.qianxun.homestay.modules.member.vo.MemberCreatedVO;
import com.qianxun.homestay.modules.member.vo.MemberPageVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** M03、M05 后台会员列表与预建接口。 */
@RestController
@Validated
@RequestMapping("/api/admin/members")
public class AdminMemberController {

    private final MemberService memberService;

    public AdminMemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    /** 按手机号筛选会员并分页。 */
    @GetMapping
    public ApiResponse<MemberPageVO> list(
            @RequestParam(name = "phone", required = false) String phone,
            @RequestParam(name = "page", defaultValue = "1") @Min(1) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(memberService.findMembers(phone, page, size));
    }

    /** 预建会员资料，初始积分由积分发放接口单独记账。 */
    @PostMapping
    public ApiResponse<MemberCreatedVO> create(@Valid @RequestBody MemberCreateRequest request) {
        return ApiResponse.success(memberService.createMember(request));
    }

}
