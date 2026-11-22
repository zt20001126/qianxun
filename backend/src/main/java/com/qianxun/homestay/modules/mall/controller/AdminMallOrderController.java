package com.qianxun.homestay.modules.mall.controller;

import com.qianxun.homestay.common.api.ApiResponse;
import com.qianxun.homestay.modules.mall.dto.MallShipRequest;
import com.qianxun.homestay.modules.mall.service.MallOrderService;
import com.qianxun.homestay.modules.mall.vo.MallOrderPageVO;
import com.qianxun.homestay.modules.mall.vo.MallOrderVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 商城管理员订单查询及状态操作接口。 */
@RestController
@Validated
@RequestMapping("/api/admin/mall/orders")
public class AdminMallOrderController {
    private final MallOrderService service;
    public AdminMallOrderController(MallOrderService service) { this.service = service; }
    @GetMapping public ApiResponse<MallOrderPageVO> list(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(service.findPage(page, size));
    }
    @GetMapping("/{id}") public ApiResponse<MallOrderVO> detail(@PathVariable Long id) {
        return ApiResponse.success(service.findById(id));
    }
    @PostMapping("/{id}/ship") public ApiResponse<MallOrderVO> ship(
            @PathVariable Long id, @Valid @RequestBody MallShipRequest request) {
        return ApiResponse.success(service.ship(id, request));
    }
    @PostMapping("/{id}/complete") public ApiResponse<MallOrderVO> complete(@PathVariable Long id) {
        return ApiResponse.success(service.complete(id));
    }
}
