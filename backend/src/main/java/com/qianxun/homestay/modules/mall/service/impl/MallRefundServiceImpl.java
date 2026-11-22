package com.qianxun.homestay.modules.mall.service.impl;

import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.modules.mall.entity.MallOrderEntity;
import com.qianxun.homestay.modules.mall.entity.MallPaymentEntity;
import com.qianxun.homestay.modules.mall.mapper.MallOrderMapper;
import com.qianxun.homestay.modules.mall.mapper.MallPaymentMapper;
import com.qianxun.homestay.modules.mall.mapper.MallRefundMapper;
import com.qianxun.homestay.modules.mall.service.MallRefundService;
import com.qianxun.homestay.modules.mall.service.MallPointsRefundService;
import com.qianxun.homestay.modules.mall.vo.MallRefundVO;
import com.qianxun.homestay.modules.member.entity.MemberEntity;
import com.qianxun.homestay.modules.member.mapper.MemberMapper;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 原子处理商城模拟退款及积分返还。 */
@Service
public class MallRefundServiceImpl implements MallRefundService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private final MallOrderMapper orders;
    private final MallPaymentMapper payments;
    private final MallRefundMapper refunds;
    private final MemberMapper members;
    private final MallPointsRefundService pointsRefund;
    public MallRefundServiceImpl(MallOrderMapper orders, MallPaymentMapper payments, MallRefundMapper refunds,
            MemberMapper members, MallPointsRefundService pointsRefund) {
        this.orders = orders; this.payments = payments; this.refunds = refunds; this.members = members; this.pointsRefund = pointsRefund;
    }
    @Override @Transactional public MallRefundVO refund(Long memberId, Long orderId) {
        // 发货与退款都先锁订单，避免重复变更同一笔订单状态。
        MallOrderEntity order = orders.findByIdForUpdate(orderId);
        if (order == null || !memberId.equals(order.getMemberId())) throw new BusinessException(HttpStatus.NOT_FOUND, "订单不存在");
        if ("REFUNDED".equals(order.getStatus())) {
            String existing = refunds.findSuccessfulNo(orderId);
            if (existing == null) throw new BusinessException(HttpStatus.CONFLICT, "未找到订单退款流水");
            return new MallRefundVO(existing, orderId, "SUCCESS", order.getPaidAmount().setScale(2).toPlainString());
        }
        if (!"PENDING_SHIPMENT".equals(order.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, "当前订单不支持自助退款，请联系商家");
        }
        MallPaymentEntity payment = payments.findByOrderIdForUpdate(orderId);
        if (payment == null || !"SUCCESS".equals(payment.getStatus()) || !"MOCK".equals(payment.getPaymentMethod())
                || payment.getAmount().compareTo(order.getPaidAmount()) != 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "订单没有可退款的模拟支付记录");
        }
        MemberEntity member = members.findByIdForUpdate(memberId);
        if (member == null) throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "会员不存在");
        String refundNo = "MR" + UUID.randomUUID().toString().replace("-", "");
        if (refunds.insertSuccess(orderId, payment.getId(), refundNo, payment.getAmount(), LocalDateTime.now(BUSINESS_ZONE)) != 1
                || orders.markRefunded(orderId) != 1) throw new BusinessException(HttpStatus.CONFLICT, "订单退款状态已变化");
        pointsRefund.returnPoints(member, order);
        return new MallRefundVO(refundNo, orderId, "SUCCESS", payment.getAmount().setScale(2).toPlainString());
    }
}
