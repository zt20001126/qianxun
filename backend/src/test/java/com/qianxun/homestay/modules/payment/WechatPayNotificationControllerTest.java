package com.qianxun.homestay.modules.payment;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.qianxun.homestay.modules.payment.controller.WechatPayNotificationController;
import com.qianxun.homestay.modules.payment.service.WechatPaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** 通过 HTTP 请求验证微信支付与退款通知入口及原始签名字段传递。 */
class WechatPayNotificationControllerTest {

    private final WechatPaymentService service = mock(WechatPaymentService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUpController() {
        mockMvc = MockMvcBuilders.standaloneSetup(new WechatPayNotificationController(service)).build();
    }

    @Test
    void paymentEndpointForwardsRawBodyAndSignatureHeaders() throws Exception {
        mockMvc.perform(post("/api/payment/wechat/notify/payment")
                        .header("Wechatpay-Serial", "serial-001")
                        .header("Wechatpay-Timestamp", "1763620000")
                        .header("Wechatpay-Nonce", "nonce-001")
                        .header("Wechatpay-Signature", "signature-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resource\":{\"ciphertext\":\"encrypted-payment\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("成功"));

        verify(service).handlePaymentNotification(
                "serial-001", "1763620000", "nonce-001", "signature-001",
                "{\"resource\":{\"ciphertext\":\"encrypted-payment\"}}");
    }

    @Test
    void refundEndpointForwardsRawBodyAndSignatureHeaders() throws Exception {
        mockMvc.perform(post("/api/payment/wechat/notify/refund")
                        .header("Wechatpay-Serial", "serial-002")
                        .header("Wechatpay-Timestamp", "1763620001")
                        .header("Wechatpay-Nonce", "nonce-002")
                        .header("Wechatpay-Signature", "signature-002")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resource\":{\"ciphertext\":\"encrypted-refund\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));

        verify(service).handleRefundNotification(
                "serial-002", "1763620001", "nonce-002", "signature-002",
                "{\"resource\":{\"ciphertext\":\"encrypted-refund\"}}");
    }

    @Test
    void missingSignatureHeaderIsRejectedBeforeServiceInvocation() throws Exception {
        mockMvc.perform(post("/api/payment/wechat/notify/payment")
                        .header("Wechatpay-Serial", "serial-001")
                        .header("Wechatpay-Timestamp", "1763620000")
                        .header("Wechatpay-Nonce", "nonce-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(service, never()).handlePaymentNotification(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }
}
