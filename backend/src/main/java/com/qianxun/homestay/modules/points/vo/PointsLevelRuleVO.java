package com.qianxun.homestay.modules.points.vo;

/** 单个等级的成长门槛与积分兑换比例。 */
public record PointsLevelRuleVO(
        /** 等级展示名称。 */
        String name,
        /** 此等级的成长积分起点。 */
        int growthPoints,
        /** 抵扣一元所需积分数。 */
        int pointsPerYuan) {
}
