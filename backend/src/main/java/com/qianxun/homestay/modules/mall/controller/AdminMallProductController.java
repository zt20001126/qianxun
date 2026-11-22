package com.qianxun.homestay.modules.mall.controller;

import com.qianxun.homestay.common.api.ApiResponse;
import com.qianxun.homestay.modules.mall.dto.MallProductSaveRequest;
import com.qianxun.homestay.modules.mall.dto.MallProductStatusRequest;
import com.qianxun.homestay.modules.mall.service.MallProductService;
import com.qianxun.homestay.modules.mall.vo.MallProductVO;
import com.qianxun.homestay.modules.mall.vo.MallProductPageVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/** 管理员商品维护接口。 */
@RestController
@Validated
@RequestMapping("/api/admin/mall/products")
public class AdminMallProductController {
    private final MallProductService service;
    public AdminMallProductController(MallProductService service) { this.service = service; }
    @GetMapping public ApiResponse<MallProductPageVO> list(@RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(service.findAdminProducts(page, size));
    }
    @PostMapping public ApiResponse<MallProductVO> create(@Valid @RequestBody MallProductSaveRequest request) {
        return ApiResponse.success(service.create(request));
    }
    @PutMapping("/{id}") public ApiResponse<MallProductVO> update(
            @PathVariable Long id, @Valid @RequestBody MallProductSaveRequest request) {
        return ApiResponse.success(service.update(id, request));
    }
    @PostMapping("/{id}/status") public ApiResponse<MallProductVO> status(
            @PathVariable Long id, @Valid @RequestBody MallProductStatusRequest request) {
        return ApiResponse.success(service.setStatus(id, request.status()));
    }
}
