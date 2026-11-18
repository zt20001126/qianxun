package com.qianxun.homestay.modules.points.controller;

import com.qianxun.homestay.common.api.ApiResponse;
import com.qianxun.homestay.common.security.AuthPrincipal;
import com.qianxun.homestay.modules.points.enums.PointsLogType;
import com.qianxun.homestay.modules.points.service.PointsManagementService;
import com.qianxun.homestay.modules.points.vo.PointsLogPageVO;
import com.qianxun.homestay.modules.points.vo.PointsRulesVO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** T01/T02 小程序积分规则与本人流水接口。 */
@RestController
@Validated
@RequestMapping("/api/app/points")
public class AppPointsController {

    private final PointsManagementService pointsManagementService;

    public AppPointsController(PointsManagementService pointsManagementService) {
        this.pointsManagementService = pointsManagementService;
    }

    /** 查询四档会员等级、兑换比例及积分有效期规则。 */
    @GetMapping("/rules")
    public ApiResponse<PointsRulesVO> rules() {
        return ApiResponse.success(pointsManagementService.getRules());
    }

    /** 只查询当前登录会员本人的积分流水。 */
    @GetMapping("/logs")
    public ApiResponse<PointsLogPageVO> logs(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(name = "type", required = false) PointsLogType type,
            @RequestParam(name = "page", defaultValue = "1") @Min(1) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(pointsManagementService.findMyLogs(principal.id(), type, page, size));
    }
}
