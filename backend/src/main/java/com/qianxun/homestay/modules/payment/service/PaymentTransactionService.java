package com.qianxun.homestay.modules.payment.service;

import com.qianxun.homestay.modules.payment.domain.PaymentAttemptResult;

/** 执行需要数据库事务和行锁保护的模拟支付写操作。 */
public interface PaymentTransactionService {

    /** 锁定支付、预订和客房记录，成功时原子创建订单及积分流水。 */
    PaymentAttemptResult pay(Long memberId, String paymentNo);
}
