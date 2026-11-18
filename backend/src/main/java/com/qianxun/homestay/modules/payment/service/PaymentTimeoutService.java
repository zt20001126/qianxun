package com.qianxun.homestay.modules.payment.service;

/** 处理超过十分钟仍未支付的锁房交易。 */
public interface PaymentTimeoutService {

    /** 锁定支付、预订、客房及会员记录后关闭到期交易。 */
    void closeExpiredPayment(Long paymentId);
}
