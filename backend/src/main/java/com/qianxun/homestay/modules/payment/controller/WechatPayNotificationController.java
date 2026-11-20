package com.qianxun.homestay.modules.payment.controller;

import com.qianxun.homestay.modules.payment.service.WechatPaymentService;
import com.qianxun.homestay.modules.payment.vo.WechatNotifyAck;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 微信支付平台回调入口，仅接收原始报文并委托业务服务验签处理。 */
@RestController
@RequestMapping("/api/payment/wechat/notify")
public class WechatPayNotificationController {

    private final WechatPaymentService wechatPaymentService;

    public WechatPayNotificationController(WechatPaymentService wechatPaymentService) {
        this.wechatPaymentService = wechatPaymentService;
    }

    /** 接收支付结果通知，原始请求体用于 SDK 验签。 */
    @PostMapping("/payment")
    public ResponseEntity<WechatNotifyAck> payment(
            @RequestHeader("Wechatpay-Serial") String serial,
            @RequestHeader("Wechatpay-Timestamp") String timestamp,
            @RequestHeader("Wechatpay-Nonce") String nonce,
            @RequestHeader("Wechatpay-Signature") String signature,
            @RequestBody String body) {
        wechatPaymentService.handlePaymentNotification(serial, timestamp, nonce, signature, body);
        return ResponseEntity.ok(WechatNotifyAck.success());
    }

    /** 接收退款结果通知，原始请求体用于 SDK 验签。 */
    @PostMapping("/refund")
    public ResponseEntity<WechatNotifyAck> refund(
            @RequestHeader("Wechatpay-Serial") String serial,
            @RequestHeader("Wechatpay-Timestamp") String timestamp,
            @RequestHeader("Wechatpay-Nonce") String nonce,
            @RequestHeader("Wechatpay-Signature") String signature,
            @RequestBody String body) {
        wechatPaymentService.handleRefundNotification(serial, timestamp, nonce, signature, body);
        return ResponseEntity.ok(WechatNotifyAck.success());
    }
}
