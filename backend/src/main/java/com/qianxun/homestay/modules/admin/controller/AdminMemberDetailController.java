package com.qianxun.homestay.modules.admin.controller;

import com.qianxun.homestay.common.api.ApiResponse;
import com.qianxun.homestay.modules.admin.service.AdminMemberDetailService;
import com.qianxun.homestay.modules.member.vo.MemberDetailVO;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** M04 后台会员详情接口，汇总会员关联的订单和积分流水。 */
@RestController
@Validated
@RequestMapping("/api/admin/members")
public class AdminMemberDetailController {

    private final AdminMemberDetailService memberDetailService;

    public AdminMemberDetailController(AdminMemberDetailService memberDetailService) {
        this.memberDetailService = memberDetailService;
    }

    /** 查询指定会员资料及按会员 ID 归属的业务记录。 */
    @GetMapping("/{id}")
    public ApiResponse<MemberDetailVO> detail(@PathVariable @Min(1) Long id) {
        return ApiResponse.success(memberDetailService.getMemberDetail(id));
    }
}
