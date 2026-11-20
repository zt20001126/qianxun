package com.qianxun.homestay.modules.payment.config;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** 微信支付商户参数及环境开关。 */
@ConfigurationProperties(prefix = "app.payment.wechat")
public class WechatPayProperties {

    /** 是否将新交易切换为微信支付；默认关闭。 */
    private boolean enabled;
    /** 微信小程序 AppID。 */
    private String appId = "";
    /** 微信小程序 AppSecret，仅保存在服务端。 */
    private String appSecret = "";
    /** 微信支付商户号。 */
    private String merchantId = "";
    /** 商户 API 证书序列号。 */
    private String merchantSerialNumber = "";
    /** 商户 API 私钥文件路径。 */
    private String privateKeyPath = "";
    /** 微信支付 APIv3 密钥。 */
    private String apiV3Key = "";
    /** 支付结果通知完整 HTTPS 地址。 */
    private String paymentNotifyUrl = "";
    /** 退款结果通知完整 HTTPS 地址。 */
    private String refundNotifyUrl = "";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }
    public String getAppSecret() { return appSecret; }
    public void setAppSecret(String appSecret) { this.appSecret = appSecret; }
    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
    public String getMerchantSerialNumber() { return merchantSerialNumber; }
    public void setMerchantSerialNumber(String merchantSerialNumber) { this.merchantSerialNumber = merchantSerialNumber; }
    public String getPrivateKeyPath() { return privateKeyPath; }
    public void setPrivateKeyPath(String privateKeyPath) { this.privateKeyPath = privateKeyPath; }
    public String getApiV3Key() { return apiV3Key; }
    public void setApiV3Key(String apiV3Key) { this.apiV3Key = apiV3Key; }
    public String getPaymentNotifyUrl() { return paymentNotifyUrl; }
    public void setPaymentNotifyUrl(String paymentNotifyUrl) { this.paymentNotifyUrl = paymentNotifyUrl; }
    public String getRefundNotifyUrl() { return refundNotifyUrl; }
    public void setRefundNotifyUrl(String refundNotifyUrl) { this.refundNotifyUrl = refundNotifyUrl; }

    /** 校验开启新微信支付所必须提供的服务端配置。 */
    public void validateEnabledConfiguration() {
        require(appId, "WECHAT_APP_ID");
        require(appSecret, "WECHAT_APP_SECRET");
        require(merchantId, "WECHAT_PAY_MCH_ID");
        require(merchantSerialNumber, "WECHAT_PAY_MERCHANT_SERIAL");
        require(privateKeyPath, "WECHAT_PAY_PRIVATE_KEY_PATH");
        require(apiV3Key, "WECHAT_PAY_API_V3_KEY");
        requireHttpsUrl(paymentNotifyUrl, "WECHAT_PAY_NOTIFY_URL");
        requireHttpsUrl(refundNotifyUrl, "WECHAT_REFUND_NOTIFY_URL");
    }

    private void requireHttpsUrl(String value, String variableName) {
        require(value, variableName);
        try {
            URI uri = URI.create(value);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                    || uri.getUserInfo() != null || uri.getFragment() != null) {
                throw new IllegalStateException(variableName + " 必须是 HTTPS 回调地址");
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(variableName + " 不是有效的 HTTPS 地址");
        }
    }

    private void require(String value, String variableName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("微信支付已启用，但未设置 " + variableName);
        }
    }
}
