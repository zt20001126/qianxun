package com.qianxun.homestay.modules.points.controller;

import com.qianxun.homestay.common.api.ApiResponse;
import com.qianxun.homestay.modules.points.dto.PointsGrantRequest;
import com.qianxun.homestay.modules.points.service.PointsManagementService;
import com.qianxun.homestay.modules.points.vo.PointsGrantVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** T05 管理员会员积分发放接口。 */
@RestController
@Validated
@RequestMapping("/api/admin/members/{id}/points")
public class AdminMemberPointsController {

    private final PointsManagementService pointsManagementService;

    public AdminMemberPointsController(PointsManagementService pointsManagementService) {
        this.pointsManagementService = pointsManagementService;
    }

    /** 为指定会员发放历史消费奖励或赠送积分。 */
    @PostMapping("/grant")
    public ApiResponse<PointsGrantVO> grant(
            @PathVariable("id") @Min(1) Long memberId,
            @Valid @RequestBody PointsGrantRequest request) {
        return ApiResponse.success(pointsManagementService.grant(memberId, request));
    }
}
