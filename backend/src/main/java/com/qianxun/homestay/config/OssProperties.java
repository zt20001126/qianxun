package com.qianxun.homestay.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 阿里云 OSS 配置；密钥仅保存在后端配置对象中，不写入日志或接口响应。
 */
@Component
@ConfigurationProperties(prefix = "app.oss")
public class OssProperties {

    private String endpoint;
    /** 用于生成浏览器可直接访问图片地址的公开域名，可配置 CDN 域名。 */
    private String publicBaseUrl;
    private String bucket;
    private String accessKeyId;
    private String accessKeySecret;

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getBucket() {
        return bucket;
    }

    public String getPublicBaseUrl() { return publicBaseUrl; }

    public void setPublicBaseUrl(String publicBaseUrl) { this.publicBaseUrl = publicBaseUrl; }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getAccessKeySecret() {
        return accessKeySecret;
    }

    public void setAccessKeySecret(String accessKeySecret) {
        this.accessKeySecret = accessKeySecret;
    }
}
