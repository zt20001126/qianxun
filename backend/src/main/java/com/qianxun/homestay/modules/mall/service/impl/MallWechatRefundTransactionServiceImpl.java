package com.qianxun.homestay.modules.mall.service.impl;

import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.modules.mall.domain.MallWechatRefundCommand;
import com.qianxun.homestay.modules.mall.entity.MallOrderEntity;
import com.qianxun.homestay.modules.mall.entity.MallPaymentEntity;
import com.qianxun.homestay.modules.mall.entity.MallRefundEntity;
import com.qianxun.homestay.modules.mall.mapper.MallOrderMapper;
import com.qianxun.homestay.modules.mall.mapper.MallPaymentMapper;
import com.qianxun.homestay.modules.mall.mapper.MallRefundMapper;
import com.qianxun.homestay.modules.mall.service.MallPointsRefundService;
import com.qianxun.homestay.modules.mall.service.MallWechatRefundTransactionService;
import com.qianxun.homestay.modules.mall.vo.MallRefundVO;
import com.qianxun.homestay.modules.member.entity.MemberEntity;
import com.qianxun.homestay.modules.member.mapper.MemberMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

/** 微信退款准备和确认结果的独立数据库事务。 */
@Service
public class MallWechatRefundTransactionServiceImpl implements MallWechatRefundTransactionService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private final MallOrderMapper orders;
    private final MallPaymentMapper payments;
    private final MallRefundMapper refunds;
    private final MemberMapper members;
    private final MallPointsRefundService points;
    private final ApplicationEventPublisher eventPublisher;
    public MallWechatRefundTransactionServiceImpl(MallOrderMapper orders, MallPaymentMapper payments,
            MallRefundMapper refunds, MemberMapper members, MallPointsRefundService points, ApplicationEventPublisher eventPublisher) {
        this.orders = orders; this.payments = payments; this.refunds = refunds; this.members = members; this.points = points; this.eventPublisher = eventPublisher;
    }
    @Override @Transactional public MallWechatRefundCommand prepare(Long memberId, Long orderId) {
        MallOrderEntity order = orders.findByIdForUpdate(orderId);
        if (order == null || !memberId.equals(order.getMemberId())) throw new BusinessException(HttpStatus.NOT_FOUND, "订单不存在");
        MallPaymentEntity payment = payments.findByOrderIdForUpdate(orderId);
        if (payment == null || !"WECHAT".equals(payment.getPaymentMethod()) || !"SUCCESS".equals(payment.getStatus())
                || payment.getTransactionId() == null || payment.getAmount().compareTo(order.getPaidAmount()) != 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "订单没有可退款的微信支付记录");
        }
        MallRefundEntity latest = refunds.findLatest(orderId);
        if ("REFUNDED".equals(order.getStatus()) && latest != null && "SUCCESS".equals(latest.getStatus())) return command(latest, payment);
        if (!"PENDING_SHIPMENT".equals(order.getStatus())) throw new BusinessException(HttpStatus.CONFLICT, "当前订单不支持自助退款，请联系商家");
        if (latest != null && "PENDING".equals(latest.getStatus())) return command(latest, payment);
        if (latest != null && "SUCCESS".equals(latest.getStatus())) throw new BusinessException(HttpStatus.CONFLICT, "退款流水与订单状态不一致");
        MallRefundEntity refund = new MallRefundEntity();
        refund.setMallOrderId(orderId); refund.setMallPaymentId(payment.getId());
        refund.setRefundNo("MR" + UUID.randomUUID().toString().replace("-", ""));
        refund.setAmount(payment.getAmount()); refund.setStatus("PENDING");
        if (refunds.insertPending(refund) != 1) throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "创建退款流水失败");
        return command(refund, payment);
    }
    @Override @Transactional public MallRefundVO complete(String refundNo, String paymentNo, String transactionId,
            BigDecimal refundAmount, BigDecimal paymentAmount, String wechatRefundId) {
        MallRefundEntity observed = refunds.findByNo(refundNo);
        if (observed == null) throw new BusinessException(HttpStatus.NOT_FOUND, "退款流水不存在");
        // 与发货及退款申请统一先锁订单，防止状态被并发改变。
        MallOrderEntity order = orders.findByIdForUpdate(observed.getMallOrderId());
        MallPaymentEntity payment = payments.findByOrderIdForUpdate(observed.getMallOrderId());
        MallRefundEntity refund = refunds.findByNoForUpdate(refundNo);
        if (order == null || payment == null || refund == null || !refund.getMallPaymentId().equals(payment.getId())
                || !"WECHAT".equals(payment.getPaymentMethod()) || !"SUCCESS".equals(payment.getStatus())
                || !payment.getPaymentNo().equals(paymentNo) || transactionId == null
                || !transactionId.equals(payment.getTransactionId())
                || refundAmount == null || paymentAmount == null || refund.getAmount().compareTo(refundAmount) != 0
                || payment.getAmount().compareTo(paymentAmount) != 0 || order.getPaidAmount().compareTo(payment.getAmount()) != 0
                || refund.getAmount().compareTo(payment.getAmount()) != 0
                || wechatRefundId == null || wechatRefundId.isBlank()) {
            throw new BusinessException(HttpStatus.CONFLICT, "微信退款结果与订单记录不一致");
        }
        if ("SUCCESS".equals(refund.getStatus())) {
            if (!wechatRefundId.equals(refund.getWechatRefundId()) || !"REFUNDED".equals(order.getStatus())) {
                throw new BusinessException(HttpStatus.CONFLICT, "重复退款通知与记录不一致");
            }
            return result(refund);
        }
        if (!"PENDING".equals(refund.getStatus()) || !"PENDING_SHIPMENT".equals(order.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, "订单或退款状态已变化");
        }
        MemberEntity member = members.findByIdForUpdate(order.getMemberId());
        if (member == null) throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "订单会员不存在");
        if (refunds.markSuccess(refund.getId(), wechatRefundId, LocalDateTime.now(BUSINESS_ZONE)) != 1
                || orders.markRefunded(order.getId()) != 1) throw new BusinessException(HttpStatus.CONFLICT, "退款状态更新失败");
        points.returnPoints(member, order);
        eventPublisher.publishEvent(new com.qianxun.homestay.modules.notification.event.OrderNotificationEvent(
                com.qianxun.homestay.modules.notification.event.OrderNotificationEvent.Type.MALL_REFUNDED, order.getId(), refund.getAmount(), null, LocalDateTime.now(BUSINESS_ZONE)));
        return new MallRefundVO(refundNo, order.getId(), "SUCCESS", refund.getAmount().setScale(2).toPlainString());
    }
    @Override @Transactional public void fail(String refundNo) {
        MallRefundEntity observed = refunds.findByNo(refundNo);
        if (observed == null) throw new BusinessException(HttpStatus.NOT_FOUND, "退款流水不存在");
        if (orders.findByIdForUpdate(observed.getMallOrderId()) == null) throw new BusinessException(HttpStatus.NOT_FOUND, "订单不存在");
        MallRefundEntity refund = refunds.findByNoForUpdate(refundNo);
        if (refund != null && "PENDING".equals(refund.getStatus())) refunds.markFailed(refund.getId());
    }
    private MallWechatRefundCommand command(MallRefundEntity refund, MallPaymentEntity payment) {
        return new MallWechatRefundCommand(refund.getRefundNo(), payment.getPaymentNo(), payment.getTransactionId(),
                refund.getAmount(), payment.getAmount(), refund.getWechatRefundId(), refund.getMallOrderId(), refund.getStatus());
    }
    private MallRefundVO result(MallRefundEntity refund) {
        return new MallRefundVO(refund.getRefundNo(), refund.getMallOrderId(), refund.getStatus(), refund.getAmount().setScale(2).toPlainString());
    }
}
