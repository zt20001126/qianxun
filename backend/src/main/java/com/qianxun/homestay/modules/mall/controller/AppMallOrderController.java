package com.qianxun.homestay.modules.mall.controller;

import com.qianxun.homestay.common.api.ApiResponse;
import com.qianxun.homestay.common.security.AuthPrincipal;
import com.qianxun.homestay.modules.mall.service.MallOrderService;
import com.qianxun.homestay.modules.mall.service.MallRefundFacadeService;
import com.qianxun.homestay.modules.mall.vo.MallOrderPageVO;
import com.qianxun.homestay.modules.mall.vo.MallRefundVO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** 会员查询本人商城订单。 */
@RestController
@Validated
@RequestMapping("/api/app/mall/orders")
public class AppMallOrderController {
    private final MallOrderService service;
    private final MallRefundFacadeService refundService;
    public AppMallOrderController(MallOrderService service, MallRefundFacadeService refundService) {
        this.service = service;
        this.refundService = refundService;
    }
    /** 分页返回当前会员的订单。 */
    @GetMapping public ApiResponse<MallOrderPageVO> list(@AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(service.findMemberPage(principal.id(), page, size));
    }
    /** 对本人待发货订单申请整单退款。 */
    @PostMapping("/{id}/refund") public ApiResponse<MallRefundVO> refund(
            @AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long id) {
        return ApiResponse.success(refundService.refund(principal.id(), id));
    }
}
