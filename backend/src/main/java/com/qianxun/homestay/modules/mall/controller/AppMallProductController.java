package com.qianxun.homestay.modules.mall.controller;

import com.qianxun.homestay.common.api.ApiResponse;
import com.qianxun.homestay.modules.mall.service.MallProductService;
import com.qianxun.homestay.modules.mall.vo.MallProductVO;
import com.qianxun.homestay.modules.mall.vo.MallProductPageVO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 会员浏览已上架商品的接口。 */
@RestController
@Validated
@RequestMapping("/api/app/mall/products")
public class AppMallProductController {
    private final MallProductService service;
    public AppMallProductController(MallProductService service) { this.service = service; }
    @GetMapping public ApiResponse<MallProductPageVO> list(@RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(service.findOnShelfProducts(page, size));
    }
    @GetMapping("/{id}") public ApiResponse<MallProductVO> detail(@PathVariable Long id) {
        return ApiResponse.success(service.getOnShelfProduct(id));
    }
}
