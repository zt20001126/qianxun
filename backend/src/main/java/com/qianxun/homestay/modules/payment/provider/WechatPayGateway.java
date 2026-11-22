package com.qianxun.homestay.modules.payment.provider;

import com.qianxun.homestay.modules.payment.config.WechatPayProperties;
import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.RSAPublicKeyConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.jsapi.model.Amount;
import com.wechat.pay.java.service.payments.jsapi.model.CloseOrderRequest;
import com.wechat.pay.java.service.payments.jsapi.model.Payer;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayWithRequestPaymentResponse;
import com.wechat.pay.java.service.payments.jsapi.model.QueryOrderByOutTradeNoRequest;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.refund.RefundService;
import com.wechat.pay.java.service.refund.model.AmountReq;
import com.wechat.pay.java.service.refund.model.CreateRequest;
import com.wechat.pay.java.service.refund.model.QueryByOutRefundNoRequest;
import com.wechat.pay.java.service.refund.model.Refund;
import com.wechat.pay.java.service.refund.model.RefundNotification;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

/** 封装微信支付 APIv3 SDK，业务层通过该适配器调用支付平台。 */
@Component
public class WechatPayGateway {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter WECHAT_TIME_FORMAT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final WechatPayProperties properties;
    private volatile ClientBundle clients;

    public WechatPayGateway(WechatPayProperties properties) {
        this.properties = properties;
        if (properties.isEnabled()) {
            clients = buildClients();
        }
    }

    /** 创建小程序预支付并返回已签名的调起参数。 */
    public PrepayWithRequestPaymentResponse createPrepay(
            String paymentNo, BigDecimal amount, String openid, LocalDateTime expiresAt) {
        return createPrepay(paymentNo, amount, openid, expiresAt, "芊浔民宿住宿费");
    }

    /** 创建带业务说明的微信预支付。 */
    public PrepayWithRequestPaymentResponse createPrepay(
            String paymentNo, BigDecimal amount, String openid, LocalDateTime expiresAt, String description) {
        PrepayRequest request = new PrepayRequest();
        request.setAppid(properties.getAppId());
        request.setMchid(properties.getMerchantId());
        request.setDescription(description);
        request.setOutTradeNo(paymentNo);
        request.setTimeExpire(expiresAt.atZone(BUSINESS_ZONE).toOffsetDateTime().format(WECHAT_TIME_FORMAT));
        request.setNotifyUrl(properties.getPaymentNotifyUrl());

        Amount requestAmount = new Amount();
        requestAmount.setTotal(toFen(amount));
        requestAmount.setCurrency("CNY");
        request.setAmount(requestAmount);

        Payer payer = new Payer();
        payer.setOpenid(openid);
        request.setPayer(payer);
        return clients().jsapiService().prepayWithRequestPayment(request);
    }

    /** 按商户支付单号查询微信侧支付状态。 */
    public Transaction queryPayment(String paymentNo) {
        QueryOrderByOutTradeNoRequest request = new QueryOrderByOutTradeNoRequest();
        request.setOutTradeNo(paymentNo);
        request.setMchid(properties.getMerchantId());
        return clients().jsapiService().queryOrderByOutTradeNo(request);
    }

    /** 请求微信关闭尚未支付的交易。 */
    public void closePayment(String paymentNo) {
        CloseOrderRequest request = new CloseOrderRequest();
        request.setOutTradeNo(paymentNo);
        request.setMchid(properties.getMerchantId());
        clients().jsapiService().closeOrder(request);
    }

    /** 发起整笔退款；重复请求沿用同一商户退款单号。 */
    public Refund createRefund(
            String paymentNo, String refundNo, BigDecimal refundAmount, BigDecimal paymentAmount, String reason) {
        CreateRequest request = new CreateRequest();
        request.setOutTradeNo(paymentNo);
        request.setOutRefundNo(refundNo);
        request.setReason(reason);
        request.setNotifyUrl(properties.getRefundNotifyUrl());

        AmountReq amount = new AmountReq();
        amount.setRefund(toFen(refundAmount).longValue());
        amount.setTotal(toFen(paymentAmount).longValue());
        amount.setCurrency("CNY");
        request.setAmount(amount);
        return clients().refundService().create(request);
    }

    /** 按商户退款单号查退款状态，供重复请求和通知丢失时对账。 */
    public Refund queryRefund(String refundNo) {
        QueryByOutRefundNoRequest request = new QueryByOutRefundNoRequest();
        request.setOutRefundNo(refundNo);
        return clients().refundService().queryByOutRefundNo(request);
    }

    /** 验签、解密并解析支付通知原始请求体。 */
    public Transaction parsePaymentNotification(RequestParam requestParam) {
        return clients().notificationParser().parse(requestParam, Transaction.class);
    }

    /** 验签、解密并解析退款通知原始请求体。 */
    public RefundNotification parseRefundNotification(RequestParam requestParam) {
        return clients().notificationParser().parse(requestParam, RefundNotification.class);
    }

    private ClientBundle clients() {
        ClientBundle current = clients;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (clients == null) {
                clients = buildClients();
            }
            return clients;
        }
    }

    private ClientBundle buildClients() {
        properties.validateEnabledConfiguration();
        // 公钥商户不能下载平台证书；同时使用公钥验签支付应答与回调。
        Config config = properties.getPublicKeyId().isBlank()
                ? new RSAAutoCertificateConfig.Builder()
                        .merchantId(properties.getMerchantId())
                        .privateKeyFromPath(properties.getPrivateKeyPath())
                        .merchantSerialNumber(properties.getMerchantSerialNumber())
                        .apiV3Key(properties.getApiV3Key())
                        .build()
                : new RSAPublicKeyConfig.Builder()
                        .merchantId(properties.getMerchantId())
                        .privateKeyFromPath(properties.getPrivateKeyPath())
                        .merchantSerialNumber(properties.getMerchantSerialNumber())
                        .apiV3Key(properties.getApiV3Key())
                        .publicKeyId(properties.getPublicKeyId())
                        .publicKeyFromPath(properties.getPublicKeyPath())
                        .build();
        return new ClientBundle(
                new JsapiServiceExtension.Builder().config(config).build(),
                new RefundService.Builder().config(config).build(),
                new NotificationParser((com.wechat.pay.java.core.notification.NotificationConfig) config));
    }

    private Integer toFen(BigDecimal amount) {
        return amount.movePointRight(2).intValueExact();
    }

    private record ClientBundle(
            JsapiServiceExtension jsapiService,
            RefundService refundService,
            NotificationParser notificationParser) {
    }
}
