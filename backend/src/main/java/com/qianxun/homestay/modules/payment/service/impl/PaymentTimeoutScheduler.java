package com.qianxun.homestay.modules.payment.service.impl;

import com.qianxun.homestay.modules.payment.mapper.PaymentMapper;
import com.qianxun.homestay.modules.payment.service.PaymentTimeoutService;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 定期发现过期支付候选项，并逐条交由独立事务完成并发安全关单。 */
@Component
public class PaymentTimeoutScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentTimeoutScheduler.class);
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final int BATCH_SIZE = 100;

    private final PaymentMapper paymentMapper;
    private final PaymentTimeoutService paymentTimeoutService;

    public PaymentTimeoutScheduler(
            PaymentMapper paymentMapper,
            PaymentTimeoutService paymentTimeoutService) {
        this.paymentMapper = paymentMapper;
        this.paymentTimeoutService = paymentTimeoutService;
    }

    /** 扫描到期交易；每笔关闭失败单独记录，不影响同批其余交易。 */
    @Scheduled(fixedDelayString = "${booking.payment-timeout-scan-interval-ms:30000}")
    public void closeExpiredPayments() {
        for (Long paymentId : paymentMapper.findExpiredPaymentIds(LocalDateTime.now(BUSINESS_ZONE), BATCH_SIZE)) {
            try {
                paymentTimeoutService.closeExpiredPayment(paymentId);
            } catch (RuntimeException exception) {
                LOGGER.error("关闭过期模拟支付失败，payment_id={}", paymentId, exception);
            }
        }
    }
}
