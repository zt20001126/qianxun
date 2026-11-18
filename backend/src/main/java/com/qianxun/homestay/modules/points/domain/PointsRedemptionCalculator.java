package com.qianxun.homestay.modules.points.domain;

import com.qianxun.homestay.common.exception.BusinessException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** 按会员成长积分及房费上限计算可用积分和抵扣金额。 */
@Component
public class PointsRedemptionCalculator {

    private static final int SILVER_LEVEL_MIN_GROWTH = 500;
    private static final int GOLD_LEVEL_MIN_GROWTH = 1000;
    private static final int DIAMOND_LEVEL_MIN_GROWTH = 1500;
    private static final int ORDINARY_POINTS_PER_YUAN = 30;
    private static final int SILVER_POINTS_PER_YUAN = 25;
    private static final int GOLD_POINTS_PER_YUAN = 20;
    private static final int DIAMOND_POINTS_PER_YUAN = 15;
    private static final BigDecimal MAX_DISCOUNT_RATE = new BigDecimal("0.20");
    private static final BigDecimal ZERO_AMOUNT = new BigDecimal("0.00");

    /** 校验积分余额及房费抵扣上限，并向下计算到分的抵扣金额。 */
    public PointsRedemptionQuote calculate(
            BigDecimal roomAmount, Integer growthPoints, int availablePoints, Integer requestedPoints) {
        int pointsUsed = requestedPoints == null ? 0 : requestedPoints;
        if (roomAmount == null || roomAmount.signum() < 0 || availablePoints < 0 || pointsUsed < 0) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "积分抵扣参数不正确");
        }

        int pointsPerYuan = pointsPerYuan(growthPoints == null ? 0 : growthPoints);
        BigDecimal pointsLimitedByRoom = roomAmount.multiply(MAX_DISCOUNT_RATE)
                .multiply(BigDecimal.valueOf(pointsPerYuan))
                .setScale(0, RoundingMode.DOWN);
        // 金额字段可大于积分整数上限；先与会员余额取小值，避免大房费导致整数溢出。
        int maxPointsUsable = pointsLimitedByRoom.min(BigDecimal.valueOf(availablePoints)).intValueExact();
        if (pointsUsed > availablePoints) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "抵扣积分超过当前可用积分");
        }
        if (BigDecimal.valueOf(pointsUsed).compareTo(pointsLimitedByRoom) > 0) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "单笔积分抵扣不能超过房费的 20%");
        }

        BigDecimal discountAmount = BigDecimal.valueOf(pointsUsed)
                .divide(BigDecimal.valueOf(pointsPerYuan), 2, RoundingMode.DOWN);
        BigDecimal payableAmount = roomAmount.subtract(discountAmount).setScale(2, RoundingMode.UNNECESSARY);
        return new PointsRedemptionQuote(
                roomAmount.setScale(2, RoundingMode.UNNECESSARY),
                availablePoints,
                pointsPerYuan,
                maxPointsUsable,
                pointsUsed,
                discountAmount,
                payableAmount);
    }

    private int pointsPerYuan(int growthPoints) {
        if (growthPoints >= DIAMOND_LEVEL_MIN_GROWTH) {
            return DIAMOND_POINTS_PER_YUAN;
        }
        if (growthPoints >= GOLD_LEVEL_MIN_GROWTH) {
            return GOLD_POINTS_PER_YUAN;
        }
        if (growthPoints >= SILVER_LEVEL_MIN_GROWTH) {
            return SILVER_POINTS_PER_YUAN;
        }
        return ORDINARY_POINTS_PER_YUAN;
    }
}
