package com.qianxun.homestay.modules.mall;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qianxun.homestay.modules.mall.domain.MallWechatRefundCommand;
import com.qianxun.homestay.modules.mall.service.MallWechatRefundTransactionService;
import com.qianxun.homestay.modules.mall.service.impl.MallWechatRefundServiceImpl;
import com.qianxun.homestay.modules.mall.vo.MallRefundVO;
import com.qianxun.homestay.modules.payment.provider.WechatPayGateway;
import com.wechat.pay.java.service.refund.model.Amount;
import com.wechat.pay.java.service.refund.model.Refund;
import com.wechat.pay.java.service.refund.model.Status;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** 验证商城微信退款保持处理中及渠道金额校验。 */
class MallWechatRefundServiceImplTest {
    @Test
    void processingDoesNotReturnPointsBeforeProviderSuccess() {
        MallWechatRefundTransactionService transactions = mock(MallWechatRefundTransactionService.class);
        WechatPayGateway gateway = mock(WechatPayGateway.class);
        var command = new MallWechatRefundCommand("MR123", "M123", "TX123", new BigDecimal("1.00"),
                new BigDecimal("1.00"), null, 4L, "PENDING");
        when(transactions.prepare(3L, 4L)).thenReturn(command);
        Refund response = new Refund(); response.setStatus(Status.PROCESSING);
        when(gateway.createRefund(eq("M123"), eq("MR123"), any(), any(), isNull())).thenReturn(response);
        var result = new MallWechatRefundServiceImpl(transactions, gateway).refund(3L, 4L);
        assertThat(result.status()).isEqualTo("PENDING");
        verify(transactions, never()).complete(any(), any(), any(), any(), any(), any());
    }
    @Test
    void confirmedProviderAmountsArePassedToSettlement() {
        MallWechatRefundTransactionService transactions = mock(MallWechatRefundTransactionService.class);
        WechatPayGateway gateway = mock(WechatPayGateway.class);
        var command = new MallWechatRefundCommand("MR123", "M123", "TX123", new BigDecimal("1.00"),
                new BigDecimal("1.00"), null, 4L, "PENDING");
        when(transactions.prepare(3L, 4L)).thenReturn(command);
        Refund response = new Refund(); response.setStatus(Status.SUCCESS); response.setOutRefundNo("MR123");
        response.setOutTradeNo("M123"); response.setTransactionId("TX123"); response.setRefundId("WXRF123");
        Amount amount = new Amount(); amount.setCurrency("CNY"); amount.setRefund(100L); amount.setTotal(100L); response.setAmount(amount);
        when(gateway.createRefund(eq("M123"), eq("MR123"), any(), any(), isNull())).thenReturn(response);
        when(transactions.complete("MR123", "M123", "TX123", new BigDecimal("1.00"), new BigDecimal("1.00"), "WXRF123"))
                .thenReturn(new MallRefundVO("MR123", 4L, "SUCCESS", "1.00"));
        assertThat(new MallWechatRefundServiceImpl(transactions, gateway).refund(3L, 4L).status()).isEqualTo("SUCCESS");
    }
}
