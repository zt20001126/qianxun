package com.qianxun.homestay.modules.mall.service.impl;

import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.common.util.ApiTime;
import com.qianxun.homestay.modules.mall.dto.MallOrderQueryRequest;
import com.qianxun.homestay.modules.mall.dto.MallShipRequest;
import com.qianxun.homestay.modules.mall.entity.MallOrderEntity;
import com.qianxun.homestay.modules.mall.enums.MallOrderStatus;
import com.qianxun.homestay.modules.mall.mapper.MallOrderMapper;
import com.qianxun.homestay.modules.mall.service.MallOrderService;
import com.qianxun.homestay.modules.mall.vo.AppMallOrderDetailVO;
import com.qianxun.homestay.modules.mall.vo.MallOrderPageVO;
import com.qianxun.homestay.modules.mall.vo.MallOrderVO;
import com.qianxun.homestay.modules.member.entity.MemberEntity;
import com.qianxun.homestay.modules.member.mapper.MemberMapper;
import com.qianxun.homestay.modules.points.entity.PointsLogEntity;
import com.qianxun.homestay.modules.points.enums.PointsLogType;
import com.qianxun.homestay.modules.points.mapper.PointsLogMapper;
import java.math.RoundingMode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 提供商城订单查询、履约状态维护及完成订单的积分奖励。 */
@Service
public class MallOrderServiceImpl implements MallOrderService {
    private final MallOrderMapper orderMapper;
    private final MemberMapper memberMapper;
    private final PointsLogMapper pointsLogMapper;

    public MallOrderServiceImpl(MallOrderMapper orderMapper, MemberMapper memberMapper, PointsLogMapper pointsLogMapper) {
        this.orderMapper = orderMapper;
        this.memberMapper = memberMapper;
        this.pointsLogMapper = pointsLogMapper;
    }
    @Override
    @Transactional(readOnly = true)
    public MallOrderPageVO findPage(MallOrderQueryRequest request) {
        MallOrderQueryRequest normalizedRequest = normalizeQuery(request);
        long offset = (long) (normalizedRequest.page() - 1) * normalizedRequest.size();
        return new MallOrderPageVO(
                orderMapper.findPage(normalizedRequest, offset).stream().map(this::toVO).toList(),
                orderMapper.count(normalizedRequest), normalizedRequest.page(), normalizedRequest.size());
    }
    @Override public MallOrderPageVO findMemberPage(Long memberId, int page, int size) {
        return new MallOrderPageVO(orderMapper.findMemberPage(memberId, (long) (page - 1) * size, size)
                .stream().map(this::toVO).toList(), orderMapper.countMember(memberId), page, size);
    }
    @Override @Transactional(readOnly = true)
    public AppMallOrderDetailVO findMemberById(Long memberId, Long id) {
        // 会员详情查询必须在 SQL 层同时限制归属，未命中统一按不存在返回。
        MallOrderEntity order = orderMapper.findMemberById(memberId, id);
        if (order == null) throw new BusinessException(HttpStatus.NOT_FOUND, "商城订单不存在");
        return toAppDetailVO(order);
    }
    @Override public MallOrderVO findById(Long id) { return toVO(requireOrder(id, false)); }

    @Override @Transactional public MallOrderVO ship(Long id, MallShipRequest request) {
        MallOrderEntity order = requireOrder(id, true);
        if (!MallOrderStatus.PENDING_SHIPMENT.name().equals(order.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, "当前订单不支持发货");
        }
        // 退款结果尚未确定时阻止发货，避免资金已退回但商品同时发出。
        if (orderMapper.hasPendingRefund(id)) {
            throw new BusinessException(HttpStatus.CONFLICT, "订单退款处理中，暂不能发货");
        }
        if (orderMapper.markShipped(id, request.expressCompany().trim(), request.trackingNo().trim()) != 1) {
            throw new BusinessException(HttpStatus.CONFLICT, "订单状态已变化");
        }
        return toVO(requireOrder(id, false));
    }

    @Override @Transactional public MallOrderVO complete(Long id) {
        MallOrderEntity order = requireOrder(id, true);
        if (MallOrderStatus.COMPLETED.name().equals(order.getStatus())) return toVO(order);
        if (!MallOrderStatus.SHIPPED.name().equals(order.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, "仅已发货订单可完成");
        }
        MemberEntity member = memberMapper.findByIdForUpdate(order.getMemberId());
        if (member == null) throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "订单会员不存在");
        int reward;
        int balance;
        int growth;
        try {
            reward = order.getPaidAmount().setScale(0, RoundingMode.DOWN).intValueExact();
            balance = Math.addExact(member.getPoints(), reward);
            growth = Math.addExact(member.getGrowthPoints(), reward);
        } catch (ArithmeticException exception) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "积分超出可记录范围");
        }
        String refNo = "ORDER_REWARD:MALL:" + id;
        if (pointsLogMapper.existsByRefNo(refNo)) {
            throw new BusinessException(HttpStatus.CONFLICT, "订单奖励流水与订单状态不一致");
        }
        if (orderMapper.markCompleted(id) != 1 || memberMapper.updateRewardBalances(member.getId(), balance, growth) != 1) {
            throw new BusinessException(HttpStatus.CONFLICT, "订单状态或积分余额更新失败");
        }
        PointsLogEntity log = new PointsLogEntity();
        log.setMemberId(member.getId());
        log.setMallOrderId(id);
        log.setType(PointsLogType.ORDER_REWARD);
        log.setPoints(reward);
        log.setGrowthPoints(reward);
        log.setBalanceAfter(balance);
        log.setAmount(order.getPaidAmount());
        log.setRefNo(refNo);
        log.setRemark("商城订单完成奖励");
        if (pointsLogMapper.insert(log) != 1) throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "积分流水写入失败");
        return toVO(requireOrder(id, false));
    }

    private MallOrderEntity requireOrder(Long id, boolean lock) {
        MallOrderEntity order = lock ? orderMapper.findByIdForUpdate(id) : orderMapper.findById(id);
        if (order == null) throw new BusinessException(HttpStatus.NOT_FOUND, "商城订单不存在");
        return order;
    }

    /** 去除空白关键词，避免空白字符意外缩小管理员的查询范围。 */
    private MallOrderQueryRequest normalizeQuery(MallOrderQueryRequest request) {
        String keyword = request.keyword() == null ? null : request.keyword().trim();
        if (keyword != null && keyword.isEmpty()) {
            keyword = null;
        }
        return new MallOrderQueryRequest(request.status(), keyword, request.page(), request.size());
    }

    private MallOrderVO toVO(MallOrderEntity order) {
        return new MallOrderVO(order.getId(), order.getOrderNo(), order.getMemberId(), order.getProductName(), order.getProductImage(),
                order.getQuantity(), order.getTotalAmount().setScale(2).toPlainString(),
                order.getPaidAmount().setScale(2).toPlainString(), order.getPointsUsed(), order.getRecipientName(),
                order.getRecipientPhone(), order.getShippingAddress(), order.getStatus(), order.getExpressCompany(),
                order.getTrackingNo(), ApiTime.fromShanghai(order.getPaidAt()), ApiTime.fromShanghai(order.getShippedAt()),
                ApiTime.fromShanghai(order.getCompletedAt()), ApiTime.fromShanghai(order.getCreatedAt()));
    }

    private AppMallOrderDetailVO toAppDetailVO(MallOrderEntity order) {
        return new AppMallOrderDetailVO(order.getId(), order.getOrderNo(), order.getProductName(), order.getProductImage(), order.getQuantity(),
                order.getTotalAmount().setScale(2).toPlainString(), order.getPaidAmount().setScale(2).toPlainString(),
                order.getPointsUsed(), order.getRecipientName(), order.getRecipientPhone(), order.getShippingAddress(),
                order.getStatus(), order.getExpressCompany(), order.getTrackingNo(), ApiTime.fromShanghai(order.getPaidAt()),
                ApiTime.fromShanghai(order.getShippedAt()), ApiTime.fromShanghai(order.getCompletedAt()),
                ApiTime.fromShanghai(order.getCreatedAt()));
    }
}
