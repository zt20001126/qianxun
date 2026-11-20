package com.qianxun.homestay.modules.refund.service.impl;

import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.modules.refund.domain.WechatRefundCommand;
import com.qianxun.homestay.modules.refund.enums.RefundStatus;
import com.qianxun.homestay.modules.refund.service.WechatRefundService;
import com.qianxun.homestay.modules.refund.service.WechatRefundTransactionService;
import com.qianxun.homestay.modules.refund.vo.RefundResultVO;
import com.qianxun.homestay.modules.payment.provider.WechatPayGateway;
import com.wechat.pay.java.service.refund.model.Refund;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** 在数据库事务外调用微信退款 API，避免网络请求占用订单行锁。 */
@Service
public class WechatRefundServiceImpl implements WechatRefundService {

    private final WechatPayGateway wechatPayGateway;
    private final WechatRefundTransactionService transactionService;

    public WechatRefundServiceImpl(
            WechatPayGateway wechatPayGateway, WechatRefundTransactionService transactionService) {
        this.wechatPayGateway = wechatPayGateway;
        this.transactionService = transactionService;
    }

    @Override
    public RefundResultVO refundMyOrder(Long memberId, Long orderId, String reason) {
        WechatRefundCommand command = transactionService.prepare(memberId, orderId, reason);
        if (command.status() == RefundStatus.SUCCESS || command.status() == RefundStatus.FAILED) {
            return new RefundResultVO(command.refundId(), command.refundNo(), command.status());
        }
        try {
            Refund providerResult = command.wechatRefundId() == null
                    ? wechatPayGateway.createRefund(command.paymentNo(), command.refundNo(), command.refundAmount(),
                            command.paymentAmount(), command.reason())
                    : wechatPayGateway.queryRefund(command.refundNo());
            return applyProviderResult(command, providerResult);
        } catch (RuntimeException exception) {
            // 微信请求超时可能发生在平台已受理之后；保留 PENDING，重试时用同一退款单号查询。
            try {
                return applyProviderResult(command, wechatPayGateway.queryRefund(command.refundNo()));
            } catch (RuntimeException queryException) {
                throw new BusinessException(HttpStatus.BAD_GATEWAY, "微信退款结果暂不可确认，请稍后查询");
            }
        }
    }

    @Override
    public void completeRefund(
            String refundNo, String paymentNo, String transactionId, java.math.BigDecimal refundAmount,
            java.math.BigDecimal paymentAmount, String wechatRefundId) {
        transactionService.complete(refundNo, paymentNo, transactionId, refundAmount, paymentAmount, wechatRefundId);
    }

    @Override
    public void failRefund(String refundNo, String reason) {
        transactionService.fail(refundNo, reason);
    }

    private RefundResultVO applyProviderResult(WechatRefundCommand command, Refund refund) {
        String status = refund == null || refund.getStatus() == null ? "" : refund.getStatus().name();
        if ("SUCCESS".equals(status)) {
            return transactionService.complete(command.refundNo(), command.paymentNo(), refund.getTransactionId(),
                    command.refundAmount(), command.paymentAmount(), refund.getRefundId());
        }
        if ("CLOSED".equals(status) || "ABNORMAL".equals(status)) {
            transactionService.fail(command.refundNo(), "微信退款状态：" + status);
            return new RefundResultVO(command.refundId(), command.refundNo(), RefundStatus.FAILED);
        }
        return new RefundResultVO(command.refundId(), command.refundNo(), RefundStatus.PENDING);
    }
}
