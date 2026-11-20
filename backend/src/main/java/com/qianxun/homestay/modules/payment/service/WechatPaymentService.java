package com.qianxun.homestay.modules.payment.service;

import com.qianxun.homestay.modules.payment.vo.WechatPrepayVO;

/** 负责小程序微信预支付和微信支付结果通知。 */
public interface WechatPaymentService {

    /**
     * 校验付款身份并创建微信小程序预支付。
     *
     * @param memberId 当前 JWT 会员 ID
     * @param paymentNo 商户支付单号
     * @param loginCode wx.login 凭证
     * @param phoneCode 微信手机号授权凭证
     * @return wx.requestPayment 调起参数
     */
    WechatPrepayVO createPrepay(Long memberId, String paymentNo, String loginCode, String phoneCode);

    /** 验签、校验并幂等处理微信支付结果通知。 */
    void handlePaymentNotification(
            String serial, String timestamp, String nonce, String signature, String body);

    /** 验签、校验并幂等处理微信退款结果通知。 */
    void handleRefundNotification(
            String serial, String timestamp, String nonce, String signature, String body);
}
