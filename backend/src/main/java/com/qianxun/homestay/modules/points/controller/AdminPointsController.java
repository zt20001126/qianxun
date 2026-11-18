package com.qianxun.homestay.modules.points.controller;

import com.qianxun.homestay.common.api.ApiResponse;
import com.qianxun.homestay.modules.points.enums.PointsLogType;
import com.qianxun.homestay.modules.points.service.PointsManagementService;
import com.qianxun.homestay.modules.points.vo.PointsLogPageVO;
import com.qianxun.homestay.modules.points.vo.PointsRewardRetryVO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** T04/T06 后台积分流水与订单奖励重试接口。 */
@RestController
@Validated
@RequestMapping("/api/admin/points")
public class AdminPointsController {

    private final PointsManagementService pointsManagementService;

    public AdminPointsController(PointsManagementService pointsManagementService) {
        this.pointsManagementService = pointsManagementService;
    }

    /** 按会员、订单和流水类型筛选后台积分流水。 */
    @GetMapping("/logs")
    public ApiResponse<PointsLogPageVO> logs(
            @RequestParam(name = "member_id", required = false) @Min(1) Long memberId,
            @RequestParam(name = "order_id", required = false) @Min(1) Long orderId,
            @RequestParam(name = "type", required = false) PointsLogType type,
            @RequestParam(name = "page", defaultValue = "1") @Min(1) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(pointsManagementService.findAdminLogs(memberId, orderId, type, page, size));
    }

    /** 对符合条件的已完成订单重试积分奖励。 */
    @PostMapping("/rewards/{orderId}/retry")
    public ApiResponse<PointsRewardRetryVO> retry(@PathVariable @Min(1) Long orderId) {
        return ApiResponse.success(pointsManagementService.retryOrderReward(orderId));
    }
}
