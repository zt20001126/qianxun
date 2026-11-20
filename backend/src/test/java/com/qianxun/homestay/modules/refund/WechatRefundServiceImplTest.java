package com.qianxun.homestay.modules.refund;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.modules.payment.provider.WechatPayGateway;
import com.qianxun.homestay.modules.refund.domain.WechatRefundCommand;
import com.qianxun.homestay.modules.refund.enums.RefundStatus;
import com.qianxun.homestay.modules.refund.service.WechatRefundTransactionService;
import com.qianxun.homestay.modules.refund.service.impl.WechatRefundServiceImpl;
import com.wechat.pay.java.service.refund.model.Refund;
import com.wechat.pay.java.service.refund.model.Status;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/** 验证微信退款同步响应、待处理、失败和网络结果不确定时的处置。 */
class WechatRefundServiceImplTest {

    private static final Long MEMBER_ID = 7L;
    private static final Long ORDER_ID = 71L;
    private static final Long REFUND_ID = 701L;
    private static final String REFUND_NO = "RF-TEST-701";
    private static final String PAYMENT_NO = "PAY-TEST-701";
    private static final BigDecimal AMOUNT = new BigDecimal("123.45");

    private final WechatPayGateway gateway = mock(WechatPayGateway.class);
    private final WechatRefundTransactionService transactionService = mock(WechatRefundTransactionService.class);
    private final WechatRefundServiceImpl service = new WechatRefundServiceImpl(gateway, transactionService);
    private final WechatRefundCommand command = new WechatRefundCommand(
            REFUND_ID, REFUND_NO, PAYMENT_NO, AMOUNT, AMOUNT, "行程有变", null, RefundStatus.PENDING);

    @BeforeEach
    void preparePendingRefund() {
        when(transactionService.prepare(MEMBER_ID, ORDER_ID, "行程有变")).thenReturn(command);
    }

    @Test
    void confirmedProviderSuccessCompletesLocalRefund() {
        when(gateway.createRefund(PAYMENT_NO, REFUND_NO, AMOUNT, AMOUNT, "行程有变"))
                .thenReturn(providerRefund(Status.SUCCESS));
        when(transactionService.complete(REFUND_NO, PAYMENT_NO, "wx-transaction-701", AMOUNT, AMOUNT,
                "wx-refund-701"))
                .thenReturn(new com.qianxun.homestay.modules.refund.vo.RefundResultVO(
                        REFUND_ID, REFUND_NO, RefundStatus.SUCCESS));

        var result = service.refundMyOrder(MEMBER_ID, ORDER_ID, "行程有变");

        assertThat(result.status()).isEqualTo(RefundStatus.SUCCESS);
        verify(transactionService).complete(REFUND_NO, PAYMENT_NO, "wx-transaction-701", AMOUNT, AMOUNT,
                "wx-refund-701");
    }

    @Test
    void processingProviderStatusKeepsRefundPending() {
        when(gateway.createRefund(PAYMENT_NO, REFUND_NO, AMOUNT, AMOUNT, "行程有变"))
                .thenReturn(providerRefund(Status.PROCESSING));

        var result = service.refundMyOrder(MEMBER_ID, ORDER_ID, "行程有变");

        assertThat(result.status()).isEqualTo(RefundStatus.PENDING);
        verify(transactionService, never()).complete(any(), any(), any(), any(), any(), any());
        verify(transactionService, never()).fail(any(), any());
    }

    @Test
    void closedProviderStatusMarksRefundFailedWithoutCompletingIt() {
        when(gateway.createRefund(PAYMENT_NO, REFUND_NO, AMOUNT, AMOUNT, "行程有变"))
                .thenReturn(providerRefund(Status.CLOSED));

        var result = service.refundMyOrder(MEMBER_ID, ORDER_ID, "行程有变");

        assertThat(result.status()).isEqualTo(RefundStatus.FAILED);
        verify(transactionService).fail(REFUND_NO, "微信退款状态：CLOSED");
        verify(transactionService, never()).complete(any(), any(), any(), any(), any(), any());
    }

    @Test
    void createTimeoutReconcilesByStableRefundNumber() {
        when(gateway.createRefund(PAYMENT_NO, REFUND_NO, AMOUNT, AMOUNT, "行程有变"))
                .thenThrow(new IllegalStateException("request timed out"));
        when(gateway.queryRefund(REFUND_NO)).thenReturn(providerRefund(Status.SUCCESS));
        when(transactionService.complete(REFUND_NO, PAYMENT_NO, "wx-transaction-701", AMOUNT, AMOUNT,
                "wx-refund-701"))
                .thenReturn(new com.qianxun.homestay.modules.refund.vo.RefundResultVO(
                        REFUND_ID, REFUND_NO, RefundStatus.SUCCESS));

        var result = service.refundMyOrder(MEMBER_ID, ORDER_ID, "行程有变");

        assertThat(result.status()).isEqualTo(RefundStatus.SUCCESS);
        verify(gateway).queryRefund(REFUND_NO);
    }

    @Test
    void unresolvedCreateAndQueryFailureLeavesRefundPendingAndReturnsGatewayError() {
        when(gateway.createRefund(PAYMENT_NO, REFUND_NO, AMOUNT, AMOUNT, "行程有变"))
                .thenThrow(new IllegalStateException("request timed out"));
        when(gateway.queryRefund(REFUND_NO)).thenThrow(new IllegalStateException("query unavailable"));

        assertThatThrownBy(() -> service.refundMyOrder(MEMBER_ID, ORDER_ID, "行程有变"))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getStatus())
                .isEqualTo(HttpStatus.BAD_GATEWAY);

        verify(transactionService, never()).fail(any(), any());
        verify(transactionService, never()).complete(any(), any(), any(), any(), any(), any());
    }

    @Test
    void retryWithExistingWechatRefundIdQueriesInsteadOfCreatingAnotherRefund() {
        WechatRefundCommand retryCommand = new WechatRefundCommand(
                REFUND_ID, REFUND_NO, PAYMENT_NO, AMOUNT, AMOUNT, "行程有变", "wx-refund-existing",
                RefundStatus.PENDING);
        when(transactionService.prepare(MEMBER_ID, ORDER_ID, "行程有变")).thenReturn(retryCommand);
        when(gateway.queryRefund(REFUND_NO)).thenReturn(providerRefund(Status.PROCESSING));

        assertThat(service.refundMyOrder(MEMBER_ID, ORDER_ID, "行程有变").status())
                .isEqualTo(RefundStatus.PENDING);

        verify(gateway).queryRefund(REFUND_NO);
        verify(gateway, never()).createRefund(any(), any(), any(), any(), any());
    }

    @Test
    void priorTerminalRefundDoesNotCallWechatAgain() {
        WechatRefundCommand terminalCommand = new WechatRefundCommand(
                REFUND_ID, REFUND_NO, PAYMENT_NO, AMOUNT, AMOUNT, "行程有变", "wx-refund-existing",
                RefundStatus.SUCCESS);
        when(transactionService.prepare(MEMBER_ID, ORDER_ID, "行程有变")).thenReturn(terminalCommand);

        assertThat(service.refundMyOrder(MEMBER_ID, ORDER_ID, "行程有变").status())
                .isEqualTo(RefundStatus.SUCCESS);

        verify(gateway, never()).queryRefund(any());
        verify(gateway, never()).createRefund(any(), any(), any(), any(), any());
    }

    private Refund providerRefund(Status status) {
        Refund refund = new Refund();
        refund.setStatus(status);
        refund.setRefundId("wx-refund-701");
        refund.setTransactionId("wx-transaction-701");
        return refund;
    }
}
