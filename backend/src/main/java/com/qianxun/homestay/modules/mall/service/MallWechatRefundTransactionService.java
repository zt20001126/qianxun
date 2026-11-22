package com.qianxun.homestay.modules.mall.service;

import com.qianxun.homestay.modules.mall.domain.MallWechatRefundCommand;
import com.qianxun.homestay.modules.mall.vo.MallRefundVO;
import java.math.BigDecimal;

/** 商城微信退款的事务化准备与结果结算。 */
public interface MallWechatRefundTransactionService {
    /** 锁定订单并创建或复用退款流水。 */ MallWechatRefundCommand prepare(Long memberId, Long orderId);
    /** 验证渠道结果并原子返还积分。 */
    MallRefundVO complete(String refundNo, String paymentNo, String transactionId,
            BigDecimal refundAmount, BigDecimal paymentAmount, String wechatRefundId);
    /** 将明确失败的退款流水标记失败。 */ void fail(String refundNo);
}
