package com.qianxun.homestay.modules.mall.controller;

import com.qianxun.homestay.common.api.ApiResponse;
import com.qianxun.homestay.modules.mall.service.MallProductImageUploadService;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 管理员上传商城商品图片接口。 */
@RestController
@RequestMapping("/api/admin/mall/products/images")
public class AdminMallProductImageController {
    private final MallProductImageUploadService service;

    public AdminMallProductImageController(MallProductImageUploadService service) { this.service = service; }

    @PostMapping(consumes = "multipart/form-data")
    public ApiResponse<Map<String, String>> upload(@RequestPart("file") MultipartFile file) {
        return ApiResponse.success(Map.of("url", service.upload(file)));
    }
}
