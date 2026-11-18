package com.qianxun.homestay.modules.admin.service.impl;

import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.common.util.ApiTime;
import com.qianxun.homestay.modules.admin.service.AdminMemberDetailService;
import com.qianxun.homestay.modules.member.domain.MemberLevelCalculator;
import com.qianxun.homestay.modules.member.entity.MemberEntity;
import com.qianxun.homestay.modules.member.mapper.MemberMapper;
import com.qianxun.homestay.modules.member.vo.MemberDetailVO;
import com.qianxun.homestay.modules.member.vo.MemberOrderVO;
import com.qianxun.homestay.modules.member.vo.MemberPointsLogVO;
import com.qianxun.homestay.modules.order.entity.OrderViewEntity;
import com.qianxun.homestay.modules.order.mapper.OrderMapper;
import com.qianxun.homestay.modules.points.entity.PointsLogEntity;
import com.qianxun.homestay.modules.points.mapper.PointsLogMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 聚合会员及其订单、积分流水，供后台会员详情接口使用。 */
@Service
public class AdminMemberDetailServiceImpl implements AdminMemberDetailService {

    private static final int MEMBER_DETAIL_POINTS_LOG_SIZE = 20;

    private final MemberMapper memberMapper;
    private final OrderMapper orderMapper;
    private final PointsLogMapper pointsLogMapper;
    private final MemberLevelCalculator levelCalculator;

    public AdminMemberDetailServiceImpl(
            MemberMapper memberMapper,
            OrderMapper orderMapper,
            PointsLogMapper pointsLogMapper,
            MemberLevelCalculator levelCalculator) {
        this.memberMapper = memberMapper;
        this.orderMapper = orderMapper;
        this.pointsLogMapper = pointsLogMapper;
        this.levelCalculator = levelCalculator;
    }

    @Override
    @Transactional(readOnly = true)
    public MemberDetailVO getMemberDetail(Long memberId) {
        MemberEntity member = memberMapper.findById(memberId);
        if (member == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "会员不存在");
        }
        List<MemberOrderVO> orders = orderMapper.findMemberOrders(memberId).stream()
                .map(this::toMemberOrder).toList();
        List<MemberPointsLogVO> pointsLogs = pointsLogMapper.findPage(
                        memberId, null, null, 0, MEMBER_DETAIL_POINTS_LOG_SIZE).stream()
                .map(this::toPointsLog).toList();
        long pointsLogTotal = pointsLogMapper.countPage(memberId, null, null);
        return new MemberDetailVO(member.getId(), member.getPhone(), member.getNickname(), member.getAvatar(),
                member.getGrowthPoints(), member.getPoints(), levelCalculator.levelFor(member.getGrowthPoints()).getDisplayName(),
                ApiTime.fromShanghai(member.getCreatedAt()), orders, pointsLogs, pointsLogTotal);
    }

    private MemberOrderVO toMemberOrder(OrderViewEntity order) {
        return new MemberOrderVO(order.getId(), order.getOrderNo(), order.getRoomNumber(), order.getCheckIn(),
                order.getCheckOut(), order.getStatus().name(),
                order.getPaidAmount().setScale(2, RoundingMode.UNNECESSARY).toPlainString());
    }

    private MemberPointsLogVO toPointsLog(PointsLogEntity log) {
        return new MemberPointsLogVO(log.getId(), log.getMemberId(), log.getOrderId(), log.getType().name(),
                log.getPoints(), log.getGrowthPoints(), money(log.getAmount()), money(log.getDiscountAmount()),
                log.getBalanceAfter(), log.getRemark(), ApiTime.fromShanghai(log.getCreatedAt()));
    }

    private String money(BigDecimal amount) {
        return amount == null ? null : amount.setScale(2).toPlainString();
    }
}
