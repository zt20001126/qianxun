package com.qianxun.homestay.modules.payment.service;

import com.qianxun.homestay.modules.payment.domain.PaymentAttemptResult;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 执行需要数据库事务和行锁保护的模拟支付写操作。 */
public interface PaymentTransactionService {

    /** 锁定支付、预订和客房记录，成功时原子创建订单及积分流水。 */
    PaymentAttemptResult pay(Long memberId, String paymentNo);

    /**
     * 根据已验签的微信支付通知结算订单与积分。
     *
     * @param paymentNo 商户支付单号
     * @param transactionId 微信支付交易号
     * @param amount 微信通知中的实付金额
     * @param paidAt 微信确认的支付时间
     * @param payerOpenid 微信通知中的付款人 OpenID
     * @return 结算结果；重复通知返回原订单
     */
    PaymentAttemptResult settleWechatPayment(
            String paymentNo, String transactionId, BigDecimal amount, LocalDateTime paidAt, String payerOpenid);
}
