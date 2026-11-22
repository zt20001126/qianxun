package com.qianxun.homestay.modules.mall.service.impl;

import com.qianxun.homestay.modules.mall.mapper.MallPaymentMapper;
import com.qianxun.homestay.modules.mall.service.MallPaymentTimeoutService;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 定期检查商城到期支付并交由事务服务关闭。 */
@Component
public class MallPaymentTimeoutScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(MallPaymentTimeoutScheduler.class);
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final int BATCH_SIZE = 100;
    private final MallPaymentMapper payments;
    private final MallPaymentTimeoutService timeout;
    public MallPaymentTimeoutScheduler(MallPaymentMapper payments, MallPaymentTimeoutService timeout) {
        this.payments = payments; this.timeout = timeout;
    }
    /** 逐笔扫描，单笔失败不影响其他交易。 */
    @Scheduled(fixedDelayString = "${booking.payment-timeout-scan-interval-ms:30000}")
    public void closeExpiredPayments() {
        for (Long id : payments.findExpiredIds(LocalDateTime.now(BUSINESS_ZONE), BATCH_SIZE)) {
            try { timeout.closeExpired(id); }
            catch (RuntimeException exception) { LOGGER.error("商城支付超时处理失败，payment_id={}", id, exception); }
        }
    }
}
