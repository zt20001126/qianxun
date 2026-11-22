package com.qianxun.homestay.modules.points.service.impl;

import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.modules.availability.service.AvailabilityService;
import com.qianxun.homestay.modules.booking.mapper.BookingLockMapper;
import com.qianxun.homestay.modules.member.entity.MemberEntity;
import com.qianxun.homestay.modules.member.mapper.MemberMapper;
import com.qianxun.homestay.modules.mall.mapper.MallPaymentReservationMapper;
import com.qianxun.homestay.modules.points.domain.PointsRedemptionCalculator;
import com.qianxun.homestay.modules.points.domain.PointsRedemptionQuote;
import com.qianxun.homestay.modules.points.dto.PointsPreviewRequest;
import com.qianxun.homestay.modules.points.entity.PointsLogEntity;
import com.qianxun.homestay.modules.points.enums.PointsLogType;
import com.qianxun.homestay.modules.points.mapper.PointsLogMapper;
import com.qianxun.homestay.modules.points.service.PointsBalanceService;
import com.qianxun.homestay.modules.points.vo.PointsPreviewVO;
import com.qianxun.homestay.modules.availability.vo.AppAvailabilityCheckVO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 汇总会员积分余额及预占数据，并在支付成功事务中完成积分扣减。 */
@Service
public class PointsBalanceServiceImpl implements PointsBalanceService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final AvailabilityService availabilityService;
    private final BookingLockMapper bookingLockMapper;
    private final MallPaymentReservationMapper mallPaymentReservationMapper;
    private final MemberMapper memberMapper;
    private final PointsLogMapper pointsLogMapper;
    private final PointsRedemptionCalculator calculator;

    public PointsBalanceServiceImpl(
            AvailabilityService availabilityService,
            BookingLockMapper bookingLockMapper,
            MallPaymentReservationMapper mallPaymentReservationMapper,
            MemberMapper memberMapper,
            PointsLogMapper pointsLogMapper,
            PointsRedemptionCalculator calculator) {
        this.availabilityService = availabilityService;
        this.bookingLockMapper = bookingLockMapper;
        this.mallPaymentReservationMapper = mallPaymentReservationMapper;
        this.memberMapper = memberMapper;
        this.pointsLogMapper = pointsLogMapper;
        this.calculator = calculator;
    }

    @Override
    @Transactional(readOnly = true)
    public PointsPreviewVO preview(Long memberId, PointsPreviewRequest request) {
        AppAvailabilityCheckVO availability = availabilityService.checkAppAvailability(
                request.roomId(), request.checkIn(), request.checkOut());
        if (!availability.available()) {
            throw new BusinessException(HttpStatus.CONFLICT, "所选日期已不可预订，请重新选择");
        }
        MemberEntity member = memberMapper.findById(memberId);
        if (member == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "会员不存在");
        }
        PointsRedemptionQuote quote = calculateForMember(
                member, new BigDecimal(availability.roomAmount()), request.pointsUsed(), now());
        return new PointsPreviewVO(
                money(quote.roomAmount()), quote.availablePoints(), quote.pointsPerYuan(),
                quote.maxPointsUsable(), quote.pointsUsed(), money(quote.discountAmount()), money(quote.payAmount()));
    }

    @Override
    @Transactional(readOnly = true)
    public PointsRedemptionQuote calculateForLockedMember(
            MemberEntity member, BigDecimal roomAmount, Integer pointsUsed, LocalDateTime now) {
        return calculateForMember(member, roomAmount, pointsUsed, now);
    }

    @Override
    @Transactional
    public void consumeForPaidOrder(Long memberId, int pointsUsed, BigDecimal discountAmount, Long orderId) {
        if (pointsUsed == 0) {
            return;
        }
        MemberEntity member = memberMapper.findByIdForUpdate(memberId);
        if (member == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "会员不存在");
        }
        int currentPoints = member.getPoints() == null ? 0 : member.getPoints();
        if (currentPoints < pointsUsed) {
            throw new BusinessException(HttpStatus.CONFLICT, "会员积分余额已变化，请重新创建支付交易");
        }
        int balanceAfter = currentPoints - pointsUsed;
        if (memberMapper.updatePointsBalance(memberId, balanceAfter) != 1) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "会员积分更新失败");
        }

        String refNo = "REDEEM:ORDER:" + orderId;
        if (pointsLogMapper.existsByRefNo(refNo)) {
            throw new BusinessException(HttpStatus.CONFLICT, "该订单积分已抵扣");
        }
        PointsLogEntity pointsLog = new PointsLogEntity();
        pointsLog.setMemberId(memberId);
        pointsLog.setOrderId(orderId);
        pointsLog.setType(PointsLogType.REDEEM);
        pointsLog.setPoints(-pointsUsed);
        pointsLog.setGrowthPoints(0);
        pointsLog.setBalanceAfter(balanceAfter);
        pointsLog.setDiscountAmount(discountAmount);
        pointsLog.setRefNo(refNo);
        pointsLog.setRemark("订单积分抵扣");
        if (pointsLogMapper.insert(pointsLog) != 1) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "积分抵扣流水写入失败");
        }
    }

    @Override
    @Transactional
    public void returnForRefund(Long memberId, Long orderId) {
        String returnRefNo = "RETURN:ORDER:" + orderId;
        if (pointsLogMapper.existsByRefNo(returnRefNo)) {
            return;
        }

        PointsLogEntity redemption = pointsLogMapper.findByRefNo("REDEEM:ORDER:" + orderId);
        if (redemption == null) {
            return;
        }
        if (!memberId.equals(redemption.getMemberId()) || redemption.getPoints() == null
                || redemption.getPoints() >= 0) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "订单积分抵扣流水不一致");
        }

        MemberEntity member = memberMapper.findByIdForUpdate(memberId);
        if (member == null) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "退款会员不存在");
        }
        int pointsToReturn = Math.negateExact(redemption.getPoints());
        int balanceAfter;
        try {
            balanceAfter = Math.addExact(member.getPoints() == null ? 0 : member.getPoints(), pointsToReturn);
        } catch (ArithmeticException exception) {
            throw new BusinessException(HttpStatus.CONFLICT, "积分余额超出可用范围，退款未完成");
        }
        if (memberMapper.updatePointsBalance(memberId, balanceAfter) != 1) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "退款积分余额更新失败");
        }

        PointsLogEntity returned = new PointsLogEntity();
        returned.setMemberId(memberId);
        returned.setOrderId(orderId);
        returned.setType(PointsLogType.RETURN);
        returned.setPoints(pointsToReturn);
        returned.setGrowthPoints(0);
        returned.setBalanceAfter(balanceAfter);
        returned.setDiscountAmount(redemption.getDiscountAmount());
        returned.setRefNo(returnRefNo);
        returned.setRemark("订单模拟退款返还抵扣积分");
        if (pointsLogMapper.insert(returned) != 1) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "退款积分流水写入失败");
        }
    }

    private PointsRedemptionQuote calculateForMember(
            MemberEntity member, BigDecimal roomAmount, Integer pointsUsed, LocalDateTime now) {
        // 微信超时但渠道结果未确认时继续保留预占，避免不同业务重复使用积分。
        long reservedPoints = (long) bookingLockMapper.sumReservedPoints(member.getId(), now)
                + mallPaymentReservationMapper.sumReservedPoints(member.getId(), now);
        int balance = member.getPoints() == null ? 0 : member.getPoints();
        int availablePoints = (int) Math.max(0L, (long) balance - reservedPoints);
        return calculator.calculate(roomAmount, member.getGrowthPoints(), availablePoints, pointsUsed);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(BUSINESS_ZONE);
    }

    private String money(BigDecimal amount) {
        return amount.setScale(2).toPlainString();
    }
}
