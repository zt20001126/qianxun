package com.qianxun.homestay.modules.invitation.controller;

import com.qianxun.homestay.common.api.ApiResponse;
import com.qianxun.homestay.modules.invitation.dto.InvitationIssuedDTO;
import com.qianxun.homestay.modules.invitation.enums.InvitationRewardFilter;
import com.qianxun.homestay.modules.invitation.service.InvitationService;
import com.qianxun.homestay.modules.invitation.vo.AdminInvitationPageVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.util.Map;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/** 管理员查询邀请奖励并登记系统外发放情况。 */
@RestController
@Validated
@RequestMapping("/api/admin/invitations")
public class AdminInvitationController {
    private final InvitationService service;
    public AdminInvitationController(InvitationService service) { this.service = service; }

    /** 分页查询邀请人、订单及奖励。 */
    @GetMapping
    public ApiResponse<AdminInvitationPageVO> list(@RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) @Pattern(regexp = "[0-9]{0,11}", message = "手机号筛选仅支持数字") String phone,
            @RequestParam(name = "reward_status", defaultValue = "ALL") InvitationRewardFilter rewardStatus) {
        return ApiResponse.success(service.adminList(page, size, phone, rewardStatus));
    }

    /** 登记系统外发放状态，不执行转账。 */
    @PutMapping("/{id}/reward-issued")
    public ApiResponse<Map<String, Object>> setIssued(@PathVariable Long id, @Valid @RequestBody InvitationIssuedDTO dto) {
        service.setIssued(id, dto.rewardIssued());
        return ApiResponse.success(Map.of());
    }
}
