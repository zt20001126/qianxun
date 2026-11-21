package com.qianxun.homestay.modules.payment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.qianxun.homestay.modules.payment.config.WechatPayProperties;
import com.qianxun.homestay.modules.payment.provider.WechatPayGateway;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** 验证启用微信支付所需回调地址只能使用 HTTPS。 */
class WechatPayPropertiesTest {

    @Test
    void publicKeyConfigurationCreatesGatewayWithoutDownloadingPlatformCertificates(@TempDir Path directory)
            throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair merchant = generator.generateKeyPair();
        KeyPair wechat = generator.generateKeyPair();
        Path privateKey = directory.resolve("merchant.pem");
        Path publicKey = directory.resolve("wechat.pem");
        Files.writeString(privateKey, pem("PRIVATE KEY", merchant.getPrivate().getEncoded()));
        Files.writeString(publicKey, pem("PUBLIC KEY", wechat.getPublic().getEncoded()));
        WechatPayProperties properties = completeProperties(
                "https://api.example.com/payment", "https://api.example.com/refund");
        properties.setPrivateKeyPath(privateKey.toString());
        properties.setPublicKeyPath(publicKey.toString());
        properties.setPublicKeyId("PUB_KEY_ID_TEST");
        properties.setApiV3Key("01234567890123456789012345678901");
        properties.setEnabled(true);
        assertThatCode(() -> new WechatPayGateway(properties)).doesNotThrowAnyException();
    }

    @Test
    void rejectsIncompletePublicKeyConfiguration() {
        WechatPayProperties properties = completeProperties(
                "https://api.example.com/payment", "https://api.example.com/refund");
        properties.setPublicKeyId("PUB_KEY_ID_TEST");
        assertThatThrownBy(properties::validateEnabledConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("WECHAT_PAY_PUBLIC_KEY_PATH");
        properties.setPublicKeyId("");
        properties.setPublicKeyPath("wechat.pem");
        assertThatThrownBy(properties::validateEnabledConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("WECHAT_PAY_PUBLIC_KEY_ID");
    }

    private String pem(String label, byte[] key) {
        return "-----BEGIN " + label + "-----\n"
                + Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(key)
                + "\n-----END " + label + "-----\n";
    }

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
