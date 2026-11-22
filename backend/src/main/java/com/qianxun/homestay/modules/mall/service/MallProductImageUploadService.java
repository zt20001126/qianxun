package com.qianxun.homestay.modules.mall.service;

import org.springframework.web.multipart.MultipartFile;

/** 商城商品图片上传业务接口。 */
public interface MallProductImageUploadService {
    /** 校验常见图片格式并上传到 OSS，返回公开访问地址。 */
    String upload(MultipartFile file);
}
