package com.qianxun.homestay.modules.mall.service.impl;

import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.modules.mall.entity.MallOrderEntity;
import com.qianxun.homestay.modules.mall.service.MallPointsRefundService;
import com.qianxun.homestay.modules.member.entity.MemberEntity;
import com.qianxun.homestay.modules.member.mapper.MemberMapper;
import com.qianxun.homestay.modules.points.entity.PointsLogEntity;
import com.qianxun.homestay.modules.points.enums.PointsLogType;
import com.qianxun.homestay.modules.points.mapper.PointsLogMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 按原抵扣流水返还商城积分，复用调用方事务。 */
@Service
public class MallPointsRefundServiceImpl implements MallPointsRefundService {
    private final MemberMapper members;
    private final PointsLogMapper logs;
    public MallPointsRefundServiceImpl(MemberMapper members, PointsLogMapper logs) {
        this.members = members; this.logs = logs;
    }
    @Override @Transactional(propagation = Propagation.MANDATORY)
    public void returnPoints(MemberEntity member, MallOrderEntity order) {
        if (order.getPointsUsed() == 0) return;
        PointsLogEntity redeemed = logs.findByRefNo("REDEEM:MALL:" + order.getId());
        if (redeemed == null || !member.getId().equals(redeemed.getMemberId())
                || !order.getId().equals(redeemed.getMallOrderId()) || redeemed.getPoints() != -order.getPointsUsed()) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "订单抵扣积分流水不一致");
        }
        int balance;
        try { balance = Math.addExact(member.getPoints(), order.getPointsUsed()); }
        catch (ArithmeticException exception) { throw new BusinessException(HttpStatus.CONFLICT, "积分余额超出范围"); }
        if (members.updatePointsBalance(member.getId(), balance) != 1) throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "积分返还失败");
        PointsLogEntity returned = new PointsLogEntity();
        returned.setMemberId(member.getId()); returned.setMallOrderId(order.getId()); returned.setType(PointsLogType.RETURN);
        returned.setPoints(order.getPointsUsed()); returned.setGrowthPoints(0); returned.setBalanceAfter(balance);
        returned.setDiscountAmount(redeemed.getDiscountAmount()); returned.setRefNo("RETURN:MALL:" + order.getId());
        returned.setRemark("商城退款返还抵扣积分");
        if (logs.insert(returned) != 1) throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "退款积分流水写入失败");
    }
}
