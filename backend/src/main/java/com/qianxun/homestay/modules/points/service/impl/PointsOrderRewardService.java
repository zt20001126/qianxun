package com.qianxun.homestay.modules.points.service.impl;

import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.modules.member.entity.MemberEntity;
import com.qianxun.homestay.modules.member.mapper.MemberMapper;
import com.qianxun.homestay.modules.order.entity.OrderAmountEntity;
import com.qianxun.homestay.modules.order.entity.OrderEntity;
import com.qianxun.homestay.modules.order.enums.OrderStatus;
import com.qianxun.homestay.modules.order.mapper.OrderAmountMapper;
import com.qianxun.homestay.modules.order.mapper.OrderMapper;
import com.qianxun.homestay.modules.order.service.OrderRewardService;
import com.qianxun.homestay.modules.points.entity.PointsLogEntity;
import com.qianxun.homestay.modules.points.enums.PointsLogType;
import com.qianxun.homestay.modules.points.mapper.PointsLogMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 为已完成订单按实付金额发放一次积分，并确保余额和流水原子一致。 */
@Service
public class PointsOrderRewardService implements OrderRewardService {

    private static final Pattern MAINLAND_PHONE = Pattern.compile("^1[3-9]\\d{9}$");

    private final OrderMapper orderMapper;
    private final OrderAmountMapper orderAmountMapper;
    private final MemberMapper memberMapper;
    private final PointsLogMapper pointsLogMapper;
    private final String defaultAvatar;
    private final Long miniProgramChannelId;

    public PointsOrderRewardService(
            OrderMapper orderMapper,
            OrderAmountMapper orderAmountMapper,
            MemberMapper memberMapper,
            PointsLogMapper pointsLogMapper,
            @Value("${app.auth.default-avatar}") String defaultAvatar,
            @Value("${app.channel.mini-program-id}") Long miniProgramChannelId) {
        this.orderMapper = orderMapper;
        this.orderAmountMapper = orderAmountMapper;
        this.memberMapper = memberMapper;
        this.pointsLogMapper = pointsLogMapper;
        this.defaultAvatar = defaultAvatar;
        this.miniProgramChannelId = miniProgramChannelId;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void rewardIfEligible(Long orderId) {
        OrderEntity order = orderMapper.findByIdForUpdate(orderId);
        if (order == null || order.getStatus() != OrderStatus.COMPLETED) {
            return;
        }
        String refNo = "ORDER_REWARD:" + orderId;
        if (pointsLogMapper.existsByRefNo(refNo)) {
            return;
        }

        MemberEntity member = resolveMember(order);
        if (member == null) {
            return;
        }
        OrderAmountEntity amount = orderAmountMapper.findByOrderId(orderId);
        if (amount == null) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "订单金额快照不存在");
        }

        int reward = rewardPoints(amount.getPaidAmount());
        int newPoints = safeAdd(member.getPoints(), reward);
        int newGrowthPoints = safeAdd(member.getGrowthPoints(), reward);
        if (memberMapper.updateRewardBalances(member.getId(), newPoints, newGrowthPoints) != 1) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "会员积分更新失败");
        }
        if (!miniProgramChannelId.equals(order.getChannelId())) {
            orderMapper.attachMemberIfMissing(orderId, member.getId());
        }

        PointsLogEntity log = new PointsLogEntity();
        log.setMemberId(member.getId());
        log.setOrderId(orderId);
        log.setType(PointsLogType.ORDER_REWARD);
        log.setPoints(reward);
        log.setGrowthPoints(reward);
        log.setBalanceAfter(newPoints);
        log.setAmount(amount.getPaidAmount());
        log.setRefNo(refNo);
        log.setRemark("订单完成奖励");
        if (pointsLogMapper.insert(log) != 1) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "订单积分奖励流水写入失败");
        }
    }

    private MemberEntity resolveMember(OrderEntity order) {
        if (miniProgramChannelId.equals(order.getChannelId())) {
            return order.getMemberId() == null ? null : memberMapper.findByIdForUpdate(order.getMemberId());
        }
        if (order.getMemberId() != null) {
            return memberMapper.findByIdForUpdate(order.getMemberId());
        }
        String phone = order.getPhone();
        if (phone == null || !MAINLAND_PHONE.matcher(phone).matches()) {
            return null;
        }
        memberMapper.ensureExternalMember(phone, phone.substring(phone.length() - 4), defaultAvatar);
        return memberMapper.findByPhoneForUpdate(phone);
    }

    private int rewardPoints(BigDecimal paidAmount) {
        try {
            return paidAmount.setScale(0, RoundingMode.DOWN).intValueExact();
        } catch (ArithmeticException exception) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "订单金额超出积分奖励计算范围");
        }
    }

    private int safeAdd(Integer current, int increment) {
        try {
            return Math.addExact(current == null ? 0 : current, increment);
        } catch (ArithmeticException exception) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "会员积分超出可记录范围");
        }
    }
}
