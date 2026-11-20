package com.qianxun.homestay.modules.payment.vo;

/** 微信支付通知端点使用的应答格式。 */
public record WechatNotifyAck(String code, String message) {

    /** 返回微信要求的成功确认。 */
    public static WechatNotifyAck success() {
        return new WechatNotifyAck("SUCCESS", "成功");
    }
}
