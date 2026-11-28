package com.qianxun.homestay.modules.mall.service.impl;

import com.qianxun.homestay.modules.mall.service.MallProductImageUploadService;
import com.qianxun.homestay.modules.storage.service.CatalogImageUploadService;
import com.qianxun.homestay.modules.storage.service.CatalogImageUploadService.Target;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** 将商城商品图片上传到商品专用目录。 */
@Service
public class MallProductImageUploadServiceImpl implements MallProductImageUploadService {
    private final CatalogImageUploadService imageUploadService;

    public MallProductImageUploadServiceImpl(CatalogImageUploadService imageUploadService) {
        this.imageUploadService = imageUploadService;
    }

    @Override
    public String upload(MultipartFile file) {
        return imageUploadService.upload(file, Target.MALL_PRODUCT);
    }
}
