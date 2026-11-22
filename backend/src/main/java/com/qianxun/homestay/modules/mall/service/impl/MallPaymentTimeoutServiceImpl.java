package com.qianxun.homestay.modules.mall.service.impl;

import com.qianxun.homestay.modules.mall.entity.MallPaymentEntity;
import com.qianxun.homestay.modules.mall.mapper.MallPaymentMapper;
import com.qianxun.homestay.modules.mall.service.MallPaymentService;
import com.qianxun.homestay.modules.mall.service.MallPaymentTimeoutService;
import com.qianxun.homestay.modules.payment.config.WechatPayProperties;
import com.qianxun.homestay.modules.payment.provider.WechatPayGateway;
import com.wechat.pay.java.service.payments.model.Transaction;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** 商城支付过期时核对微信状态，并事务化关闭本地交易。 */
@Service
public class MallPaymentTimeoutServiceImpl implements MallPaymentTimeoutService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MallPaymentTimeoutServiceImpl.class);
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final String CREATING = "__CREATING__";
    private final MallPaymentMapper payments;
    private final MallPaymentService settlement;
    private final WechatPayGateway gateway;
    private final WechatPayProperties properties;
    private final TransactionTemplate transactions;
    public MallPaymentTimeoutServiceImpl(MallPaymentMapper payments, MallPaymentService settlement,
            WechatPayGateway gateway, WechatPayProperties properties, PlatformTransactionManager manager) {
        this.payments = payments; this.settlement = settlement; this.gateway = gateway;
        this.properties = properties; this.transactions = new TransactionTemplate(manager);
    }
    @Override public void closeExpired(Long paymentId) {
        MallPaymentEntity observed = payments.findById(paymentId);
        if (observed == null || !"PENDING".equals(observed.getStatus()) || observed.getExpiresAt().isAfter(now())) return;
        String prepayId = observed.getWechatPrepayId();
        if ("WECHAT".equals(observed.getPaymentMethod())) {
            // 渠道请求是否已受理尚不明确时不能释放积分，等待人工核查或后续结果。
            if (CREATING.equals(prepayId) && !reconcileCreating(observed)) {
                LOGGER.warn("商城微信预支付结果暂未确认，继续保留积分预占，payment_id={}", paymentId);
                payments.touchPending(paymentId);
                return;
            }
            if (prepayId != null && !CREATING.equals(prepayId) && !closeAtWechat(observed)) {
                LOGGER.warn("商城微信支付状态暂未确认或已处理，payment_id={}", paymentId);
                payments.touchPending(paymentId);
                return;
            }
        }
        transactions.executeWithoutResult(status -> {
            MallPaymentEntity locked = payments.findByIdForUpdate(paymentId);
            if (locked == null || !"PENDING".equals(locked.getStatus()) || locked.getExpiresAt().isAfter(now())) return;
            // 锁定后再次确认预支付标记，防止在渠道请求发起期间释放预占。
            if (!java.util.Objects.equals(prepayId, locked.getWechatPrepayId())) return;
            payments.markClosed(paymentId);
        });
    }
    private boolean closeAtWechat(MallPaymentEntity payment) {
        try { gateway.closePayment(payment.getPaymentNo()); return true; }
        catch (RuntimeException closeException) {
            return queryChannel(payment, false);
        }
    }
    private boolean reconcileCreating(MallPaymentEntity payment) { return queryChannel(payment, true); }
    private boolean queryChannel(MallPaymentEntity payment, boolean closeIfUnpaid) {
        try {
            Transaction result = gateway.queryPayment(payment.getPaymentNo());
            if (result == null || result.getTradeState() == null
                    || !properties.getAppId().equals(result.getAppid()) || !properties.getMerchantId().equals(result.getMchid())
                    || !payment.getPaymentNo().equals(result.getOutTradeNo())) return false;
            if ("CLOSED".equals(result.getTradeState().name())) return true;
            if (closeIfUnpaid && "NOTPAY".equals(result.getTradeState().name())) {
                gateway.closePayment(payment.getPaymentNo());
                return true;
            }
            if (!"SUCCESS".equals(result.getTradeState().name()) || result.getAmount() == null
                    || result.getAmount().getTotal() == null || !"CNY".equals(result.getAmount().getCurrency())
                    || result.getPayer() == null || result.getPayer().getOpenid() == null
                    || result.getSuccessTime() == null) return false;
            settlement.settleWechat(result.getOutTradeNo(), result.getTransactionId(),
                    BigDecimal.valueOf(result.getAmount().getTotal(), 2),
                    OffsetDateTime.parse(result.getSuccessTime()).atZoneSameInstant(BUSINESS_ZONE).toLocalDateTime(),
                    result.getPayer().getOpenid());
            return false;
        } catch (RuntimeException queryException) { return false; }
    }
    private LocalDateTime now() { return LocalDateTime.now(BUSINESS_ZONE); }
}
