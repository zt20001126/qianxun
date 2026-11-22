package com.qianxun.homestay.modules.mall.service;

import com.qianxun.homestay.modules.mall.vo.MallRefundVO;
import java.math.BigDecimal;

/** 商城微信退款渠道编排。 */
public interface MallWechatRefundService {
    /** 为本人待发货订单申请退款。 */ MallRefundVO refund(Long memberId, Long orderId);
    /** 按已验证渠道结果完成退款。 */
    void complete(String refundNo, String paymentNo, String transactionId, BigDecimal refundAmount,
            BigDecimal paymentAmount, String wechatRefundId);
    /** 处理微信明确失败结果。 */ void fail(String refundNo);
}
