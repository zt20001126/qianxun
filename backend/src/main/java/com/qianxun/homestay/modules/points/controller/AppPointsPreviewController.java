package com.qianxun.homestay.modules.points.controller;

import com.qianxun.homestay.common.api.ApiResponse;
import com.qianxun.homestay.common.security.AuthPrincipal;
import com.qianxun.homestay.modules.points.dto.PointsPreviewRequest;
import com.qianxun.homestay.modules.points.service.PointsBalanceService;
import com.qianxun.homestay.modules.points.vo.PointsPreviewVO;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** T03 小程序积分抵扣预览接口。 */
@RestController
@RequestMapping("/api/app/points")
public class AppPointsPreviewController {

    private final PointsBalanceService pointsBalanceService;

    public AppPointsPreviewController(PointsBalanceService pointsBalanceService) {
        this.pointsBalanceService = pointsBalanceService;
    }

    /** 返回扣除其他有效锁房预占后的积分试算结果。 */
    @PostMapping("/preview")
    public ApiResponse<PointsPreviewVO> preview(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody PointsPreviewRequest request) {
        return ApiResponse.success(pointsBalanceService.preview(principal.id(), request));
    }
}
