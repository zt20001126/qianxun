package com.qianxun.homestay.modules.mall.service;

/** 商城支付超时及渠道对账。 */
public interface MallPaymentTimeoutService {
    /** 处理一笔到期交易，仅在渠道明确关闭后释放微信预占。 */ void closeExpired(Long paymentId);
}
