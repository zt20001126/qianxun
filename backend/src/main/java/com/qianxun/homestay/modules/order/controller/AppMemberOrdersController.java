package com.qianxun.homestay.modules.order.controller;

import com.qianxun.homestay.common.api.ApiResponse;
import com.qianxun.homestay.common.security.AuthPrincipal;
import com.qianxun.homestay.modules.order.service.AppOrderService;
import com.qianxun.homestay.modules.order.vo.AppOrderPageVO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** M02 按当前会员归属查询消费订单。 */
@RestController
@Validated
@RequestMapping("/api/app/members/me/orders")
public class AppMemberOrdersController {

    private final AppOrderService appOrderService;

    public AppMemberOrdersController(AppOrderService appOrderService) {
        this.appOrderService = appOrderService;
    }

    /** 分页查询与当前登录会员账号关联的消费订单。 */
    @GetMapping
    public ApiResponse<AppOrderPageVO> orders(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(name = "page", defaultValue = "1") @Min(1) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(appOrderService.findMyOrders(principal.id(), null, page, size));
    }
}
