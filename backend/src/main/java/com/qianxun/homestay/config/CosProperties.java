package com.qianxun.homestay.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 腾讯云 COS 独立配置；供存储服务使用，密钥仅由后端环境提供。 */
@Component
@ConfigurationProperties(prefix = "app.cos")
public class CosProperties {
    private String region;
    private String bucket;
    private String secretId;
    private String secretKey;
    /** 公开对象的可选访问域名，不改变对象访问权限。 */
    private String publicBaseUrl;

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getBucket() { return bucket; }
    public void setBucket(String bucket) { this.bucket = bucket; }
    public String getSecretId() { return secretId; }
    public void setSecretId(String secretId) { this.secretId = secretId; }
    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
    public String getPublicBaseUrl() { return publicBaseUrl; }
    public void setPublicBaseUrl(String publicBaseUrl) { this.publicBaseUrl = publicBaseUrl; }
}
