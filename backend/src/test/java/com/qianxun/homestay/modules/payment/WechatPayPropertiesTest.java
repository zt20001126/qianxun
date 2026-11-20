package com.qianxun.homestay.modules.payment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.qianxun.homestay.modules.payment.config.WechatPayProperties;
import org.junit.jupiter.api.Test;

/** 验证启用微信支付所需回调地址只能使用 HTTPS。 */
class WechatPayPropertiesTest {

    @Test
    void validatesCompleteHttpsConfiguration() {
        WechatPayProperties properties = completeProperties(
                "https://api.example.com/api/payment/wechat/notify/payment",
                "https://api.example.com/api/payment/wechat/notify/refund");

        assertThatCode(properties::validateEnabledConfiguration).doesNotThrowAnyException();
    }

    @Test
    void rejectsHttpCallbackAddress() {
        WechatPayProperties properties = completeProperties(
                "http://api.example.com/api/payment/wechat/notify/payment",
                "https://api.example.com/api/payment/wechat/notify/refund");

        assertThatThrownBy(properties::validateEnabledConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("WECHAT_PAY_NOTIFY_URL");
    }

    @Test
    void rejectsMissingMerchantConfigurationWhenWechatPayIsEnabled() {
        WechatPayProperties properties = completeProperties(
                "https://api.example.com/api/payment/wechat/notify/payment",
                "https://api.example.com/api/payment/wechat/notify/refund");
        properties.setApiV3Key(" ");

        assertThatThrownBy(properties::validateEnabledConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("WECHAT_PAY_API_V3_KEY");
    }

    @Test
    void rejectsHttpsCallbackUrlWithUserInfoOrFragment() {
        WechatPayProperties withUserInfo = completeProperties(
                "https://user@api.example.com/api/payment/wechat/notify/payment",
                "https://api.example.com/api/payment/wechat/notify/refund");
        WechatPayProperties withFragment = completeProperties(
                "https://api.example.com/api/payment/wechat/notify/payment#fragment",
                "https://api.example.com/api/payment/wechat/notify/refund");

        assertThatThrownBy(withUserInfo::validateEnabledConfiguration)
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(withFragment::validateEnabledConfiguration)
                .isInstanceOf(IllegalStateException.class);
    }

    private WechatPayProperties completeProperties(String paymentUrl, String refundUrl) {
        WechatPayProperties properties = new WechatPayProperties();
        properties.setAppId("wx-app-id");
        properties.setAppSecret("app-secret");
        properties.setMerchantId("merchant-id");
        properties.setMerchantSerialNumber("merchant-serial");
        properties.setPrivateKeyPath("private-key.pem");
        properties.setApiV3Key("api-v3-key");
        properties.setPaymentNotifyUrl(paymentUrl);
        properties.setRefundNotifyUrl(refundUrl);
        return properties;
    }
}
