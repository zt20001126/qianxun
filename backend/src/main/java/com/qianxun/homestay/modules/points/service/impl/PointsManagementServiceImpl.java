package com.qianxun.homestay.modules.points.service.impl;

import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.common.util.ApiTime;
import com.qianxun.homestay.modules.member.domain.MemberLevelCalculator;
import com.qianxun.homestay.modules.member.entity.MemberEntity;
import com.qianxun.homestay.modules.member.enums.MemberLevel;
import com.qianxun.homestay.modules.member.mapper.MemberMapper;
import com.qianxun.homestay.modules.order.entity.OrderEntity;
import com.qianxun.homestay.modules.order.enums.OrderStatus;
import com.qianxun.homestay.modules.order.mapper.OrderMapper;
import com.qianxun.homestay.modules.order.service.OrderRewardService;
import com.qianxun.homestay.modules.points.dto.PointsGrantRequest;
import com.qianxun.homestay.modules.points.entity.PointsLogEntity;
import com.qianxun.homestay.modules.points.enums.PointsLogType;
import com.qianxun.homestay.modules.points.mapper.PointsLogMapper;
import com.qianxun.homestay.modules.points.service.PointsManagementService;
import com.qianxun.homestay.modules.points.vo.PointsGrantVO;
import com.qianxun.homestay.modules.points.vo.PointsLevelRuleVO;
import com.qianxun.homestay.modules.points.vo.PointsLogListItemVO;
import com.qianxun.homestay.modules.points.vo.PointsLogPageVO;
import com.qianxun.homestay.modules.points.vo.PointsRewardRetryVO;
import com.qianxun.homestay.modules.points.vo.PointsRulesVO;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 在数据库事务内维护积分余额、积分流水与订单奖励防重。 */
@Service
public class PointsManagementServiceImpl implements PointsManagementService {

    private static final String GRANT_REF_PREFIX = "ADMIN_GRANT:";
    private static final String ORDER_REWARD_REF_PREFIX = "ORDER_REWARD:";
    private static final BigDecimal MAX_DISCOUNT_RATE = new BigDecimal("0.20");

    private final MemberMapper memberMapper;
    private final PointsLogMapper pointsLogMapper;
    private final OrderMapper orderMapper;
    private final OrderRewardService orderRewardService;
    private final MemberLevelCalculator levelCalculator;

    public PointsManagementServiceImpl(
            MemberMapper memberMapper,
            PointsLogMapper pointsLogMapper,
            OrderMapper orderMapper,
            OrderRewardService orderRewardService,
            MemberLevelCalculator levelCalculator) {
        this.memberMapper = memberMapper;
        this.pointsLogMapper = pointsLogMapper;
        this.orderMapper = orderMapper;
        this.orderRewardService = orderRewardService;
        this.levelCalculator = levelCalculator;
    }

    @Override
    @Transactional(readOnly = true)
    public PointsRulesVO getRules() {
        List<PointsLevelRuleVO> levels = levelCalculator.levels().stream()
                .map(this::toRule)
                .toList();
        return new PointsRulesVO(levels, MAX_DISCOUNT_RATE, false);
    }

    @Override
    @Transactional(readOnly = true)
    public PointsLogPageVO findMyLogs(Long memberId, PointsLogType type, int page, int size) {
        return findLogs(memberId, null, type, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public PointsLogPageVO findAdminLogs(Long memberId, Long orderId, PointsLogType type, int page, int size) {
        return findLogs(memberId, orderId, type, page, size);
    }

    @Override
    @Transactional
    public PointsGrantVO grant(Long memberId, PointsGrantRequest request) {
        if (request.type() != PointsLogType.HISTORY_REWARD && request.type() != PointsLogType.GIFT) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "仅支持历史消费补发或赠送积分");
        }
        if (request.orderId() != null && orderMapper.findById(request.orderId()) == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "关联订单不存在");
        }
        MemberEntity member = memberMapper.findByIdForUpdate(memberId);
        if (member == null) throw new BusinessException(HttpStatus.NOT_FOUND, "会员不存在");

        String refNo = GRANT_REF_PREFIX + request.requestNo().trim();
        PointsLogEntity previous = pointsLogMapper.findByRefNo(refNo);
        if (previous != null) return replayGrant(memberId, request, previous);

        int currentPoints = member.getPoints() == null ? 0 : member.getPoints();
        int currentGrowth = member.getGrowthPoints() == null ? 0 : member.getGrowthPoints();
        int growthDelta = request.type() == PointsLogType.HISTORY_REWARD ? request.points() : 0;
        int balanceAfter = safeAdd(currentPoints, request.points());
        int growthAfter = safeAdd(currentGrowth, growthDelta);
        if (memberMapper.updateRewardBalances(memberId, balanceAfter, growthAfter) != 1) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "会员积分更新失败");
        }

        PointsLogEntity log = new PointsLogEntity();
        log.setMemberId(memberId);
        log.setOrderId(request.orderId());
        log.setType(request.type());
        log.setPoints(request.points());
        log.setGrowthPoints(growthDelta);
        log.setBalanceAfter(balanceAfter);
        log.setRefNo(refNo);
        log.setRemark(normalizeRemark(request.remark()));
        try {
            if (pointsLogMapper.insert(log) != 1) {
                throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "积分发放流水写入失败");
            }
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(HttpStatus.CONFLICT, "请求编号已用于其他积分发放请求");
        }
        return new PointsGrantVO(memberId, request.points(), growthDelta, balanceAfter, log.getId());
    }

    @Override
    @Transactional
    public PointsRewardRetryVO retryOrderReward(Long orderId) {
        OrderEntity order = orderMapper.findByIdForUpdate(orderId);
        if (order == null) throw new BusinessException(HttpStatus.NOT_FOUND, "订单不存在");
        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "仅已完成订单可重试积分奖励");
        }
        orderRewardService.rewardIfEligible(orderId);
        PointsLogEntity rewardLog = pointsLogMapper.findByRefNo(ORDER_REWARD_REF_PREFIX + orderId);
        return new PointsRewardRetryVO(orderId, rewardLog != null,
                rewardLog == null ? null : rewardLog.getId());
    }

    private PointsLogPageVO findLogs(Long memberId, Long orderId, PointsLogType type, int page, int size) {
        long offset = (long) (page - 1) * size;
        String typeValue = type == null ? null : type.name();
        List<PointsLogListItemVO> list = pointsLogMapper.findPage(
                        memberId, orderId, typeValue, offset, size).stream()
                .map(this::toListItem)
                .toList();
        long total = pointsLogMapper.countPage(memberId, orderId, typeValue);
        return new PointsLogPageVO(list, total, page, size);
    }

    private PointsLogListItemVO toListItem(PointsLogEntity log) {
        return new PointsLogListItemVO(log.getId(), log.getMemberId(), log.getOrderId(), log.getType().name(),
                log.getPoints(), log.getGrowthPoints(), money(log.getAmount()), money(log.getDiscountAmount()),
                log.getBalanceAfter(), log.getRemark(), ApiTime.fromShanghai(log.getCreatedAt()));
    }

    private PointsLevelRuleVO toRule(MemberLevel level) {
        return new PointsLevelRuleVO(level.getDisplayName(), level.getGrowthThreshold(), level.getPointsPerYuan());
    }

    private PointsGrantVO replayGrant(Long memberId, PointsGrantRequest request, PointsLogEntity previous) {
        int requestedGrowth = request.type() == PointsLogType.HISTORY_REWARD ? request.points() : 0;
        if (!memberId.equals(previous.getMemberId()) || previous.getType() != request.type()
                || !request.points().equals(previous.getPoints())
                || !java.util.Objects.equals(request.orderId(), previous.getOrderId())
                || !java.util.Objects.equals(normalizeRemark(request.remark()), previous.getRemark())
                || !java.util.Objects.equals(requestedGrowth, previous.getGrowthPoints())) {
            throw new BusinessException(HttpStatus.CONFLICT, "请求编号已用于其他积分发放内容");
        }
        return new PointsGrantVO(memberId, previous.getPoints(), previous.getGrowthPoints(),
                previous.getBalanceAfter(), previous.getId());
    }

    private String normalizeRemark(String remark) {
        return remark == null || remark.isBlank() ? null : remark.trim();
    }

    private int safeAdd(int current, int increment) {
        try {
            return Math.addExact(current, increment);
        } catch (ArithmeticException exception) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "会员积分超出可记录范围");
        }
    }

    private String money(BigDecimal amount) {
        return amount == null ? null : amount.setScale(2).toPlainString();
    }
}
