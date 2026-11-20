package com.qianxun.homestay.modules.refund.service;

import com.qianxun.homestay.modules.refund.domain.WechatRefundCommand;
import com.qianxun.homestay.modules.refund.vo.RefundResultVO;
import java.math.BigDecimal;

/** 执行微信退款准备和退款结果的本地事务。 */
public interface WechatRefundTransactionService {

    /** 锁定本人订单并创建或复用待处理退款流水。 */
    WechatRefundCommand prepare(Long memberId, Long orderId, String reason);

    /** 验证微信退款结果并原子更新流水、订单和积分。 */
    RefundResultVO complete(
            String refundNo, String paymentNo, String transactionId,
            BigDecimal refundAmount, BigDecimal paymentAmount,
            String wechatRefundId);

    /** 记录微信明确拒绝或关闭的退款结果。 */
    void fail(String refundNo, String reason);
}
