package com.qianxun.homestay.modules.points.vo;

import java.math.BigDecimal;
import java.util.List;

/** 小程序积分等级与兑换规则。 */
public record PointsRulesVO(
        /** 四档等级、成长门槛和兑换率。 */
        List<PointsLevelRuleVO> levels,
        /** 每笔订单积分抵扣占房费的最大比例。 */
        BigDecimal maxDiscountRate,
        /** 积分是否设置有效期；第一期为否。 */
        boolean expire) {
}
