package com.qianxun.homestay.modules.storage.service;

import java.io.InputStream;
import java.time.Duration;

/** 腾讯云对象存储的内部访问能力，不负责文件公开授权。 */
public interface CosStorageService {
    /** 上传指定对象；输入流由调用方负责关闭。 */
    void upload(String objectKey, InputStream input, long contentLength, String contentType);

    /** 生成不含签名的对象地址；仅公开对象可匿名访问。 */
    String publicUrl(String objectKey);

    /** 为私有对象生成指定有效期的 HTTPS 读取地址。 */
    String signedReadUrl(String objectKey, Duration lifetime);
}
