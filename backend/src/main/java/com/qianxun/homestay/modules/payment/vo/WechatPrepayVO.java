package com.qianxun.homestay.modules.payment.vo;

import com.fasterxml.jackson.annotation.JsonProperty;

/** 微信小程序 wx.requestPayment 所需的服务端签名参数。 */
public record WechatPrepayVO(
        /** 发起支付的小程序 AppID。 */
        String appId,
        /** 秒级 Unix 时间戳。 */
        String timeStamp,
        /** 本次支付随机串。 */
        String nonceStr,
        /** 预支付会话标识，字段名按微信小程序 API 契约返回。 */
        @JsonProperty("package") String packageValue,
        /** 微信支付签名算法。 */
        String signType,
        /** 服务端使用商户私钥生成的调起支付签名。 */
        String paySign) {
}
