package com.qianxun.homestay.modules.mall.service.impl;

import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.modules.mall.domain.MallWechatRefundCommand;
import com.qianxun.homestay.modules.mall.service.MallWechatRefundService;
import com.qianxun.homestay.modules.mall.service.MallWechatRefundTransactionService;
import com.qianxun.homestay.modules.mall.vo.MallRefundVO;
import com.qianxun.homestay.modules.payment.provider.WechatPayGateway;
import com.wechat.pay.java.service.refund.model.Refund;
import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** 在事务外调用微信退款并按渠道确认结果推进本地事务。 */
@Service
public class MallWechatRefundServiceImpl implements MallWechatRefundService {
    private final MallWechatRefundTransactionService transactions;
    private final WechatPayGateway gateway;
    public MallWechatRefundServiceImpl(MallWechatRefundTransactionService transactions, WechatPayGateway gateway) {
        this.transactions = transactions; this.gateway = gateway;
    }
    @Override public MallRefundVO refund(Long memberId, Long orderId) {
        MallWechatRefundCommand command = transactions.prepare(memberId, orderId);
        if ("SUCCESS".equals(command.status())) return result(command, "SUCCESS");
        try {
            Refund response = command.wechatRefundId() == null
                    ? gateway.createRefund(command.paymentNo(), command.refundNo(), command.refundAmount(), command.paymentAmount(), null)
                    : gateway.queryRefund(command.refundNo());
            return apply(command, response);
        } catch (RuntimeException exception) {
            // 请求超时不能当作失败，使用同一退款单号查询，未知时保留处理中。
            try { return apply(command, gateway.queryRefund(command.refundNo())); }
            catch (RuntimeException queryException) { throw new BusinessException(HttpStatus.BAD_GATEWAY, "退款结果暂未确认，请稍后查询"); }
        }
    }
    @Override public void complete(String refundNo, String paymentNo, String transactionId,
            BigDecimal refundAmount, BigDecimal paymentAmount, String wechatRefundId) {
        transactions.complete(refundNo, paymentNo, transactionId, refundAmount, paymentAmount, wechatRefundId);
    }
    @Override public void fail(String refundNo) { transactions.fail(refundNo); }
    private MallRefundVO apply(MallWechatRefundCommand command, Refund response) {
        String state = response == null || response.getStatus() == null ? "" : response.getStatus().name();
        if ("SUCCESS".equals(state)) {
            if (!command.refundNo().equals(response.getOutRefundNo())
                    || !command.paymentNo().equals(response.getOutTradeNo())
                    || response.getAmount() == null || response.getAmount().getRefund() == null
                    || response.getAmount().getTotal() == null || !"CNY".equals(response.getAmount().getCurrency())) {
                throw new BusinessException(HttpStatus.CONFLICT, "微信退款结果字段不一致");
            }
            return transactions.complete(command.refundNo(), command.paymentNo(), response.getTransactionId(),
                    BigDecimal.valueOf(response.getAmount().getRefund(), 2),
                    BigDecimal.valueOf(response.getAmount().getTotal(), 2), response.getRefundId());
        }
        if ("CLOSED".equals(state) || "ABNORMAL".equals(state)) {
            if (!command.refundNo().equals(response.getOutRefundNo())) {
                throw new BusinessException(HttpStatus.CONFLICT, "微信退款单号不一致");
            }
            transactions.fail(command.refundNo());
            return result(command, "FAILED");
        }
        return result(command, "PENDING");
    }
    private MallRefundVO result(MallWechatRefundCommand command, String status) {
        return new MallRefundVO(command.refundNo(), command.orderId(), status, command.refundAmount().setScale(2).toPlainString());
    }
}
