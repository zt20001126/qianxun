package com.qianxun.homestay.modules.order.controller;

import com.qianxun.homestay.common.api.ApiResponse;
import com.qianxun.homestay.common.security.AuthPrincipal;
import com.qianxun.homestay.modules.order.enums.OrderStatus;
import com.qianxun.homestay.modules.order.service.AppOrderService;
import com.qianxun.homestay.modules.order.vo.AppOrderDetailVO;
import com.qianxun.homestay.modules.order.vo.AppOrderPageVO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** O01/O02 小程序本人订单查询接口。 */
@RestController
@Validated
@RequestMapping("/api/app/orders")
public class AppOrderController {

    private final AppOrderService appOrderService;

    public AppOrderController(AppOrderService appOrderService) {
        this.appOrderService = appOrderService;
    }

    /** 分页查询当前登录会员的正式订单。 */
    @GetMapping
    public ApiResponse<AppOrderPageVO> list(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(name = "status", required = false) OrderStatus status,
            @RequestParam(name = "page", defaultValue = "1") @Min(1) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(appOrderService.findMyOrders(principal.id(), status, page, size));
    }

    /** 查询订单详情，其他会员订单统一按不存在处理。 */
    @GetMapping("/{id}")
    public ApiResponse<AppOrderDetailVO> detail(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long id) {
        return ApiResponse.success(appOrderService.findMyOrder(principal.id(), id));
    }
}
