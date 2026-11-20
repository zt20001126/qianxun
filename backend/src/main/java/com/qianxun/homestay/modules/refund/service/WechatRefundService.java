package com.qianxun.homestay.modules.refund.service;

import com.qianxun.homestay.modules.refund.vo.RefundResultVO;

/** 编排微信退款请求及退款状态核对。 */
public interface WechatRefundService {

    /** 为本人微信支付订单申请整笔退款，退款结果由微信回调确认。 */
    RefundResultVO refundMyOrder(Long memberId, Long orderId, String reason);

    /** 处理已验签且状态为成功的微信退款通知。 */
    void completeRefund(
            String refundNo, String paymentNo, String transactionId, java.math.BigDecimal refundAmount,
            java.math.BigDecimal paymentAmount, String wechatRefundId);

    /** 记录微信侧明确失败的退款通知。 */
    void failRefund(String refundNo, String reason);
}
