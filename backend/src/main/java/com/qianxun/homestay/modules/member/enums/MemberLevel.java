package com.qianxun.homestay.modules.member.enums;

/** 会员等级的固定展示信息与积分兑换率。 */
public enum MemberLevel {
    /** 0 至 499 成长积分。 */
    ORDINARY("普通", 0, 30),
    /** 500 至 999 成长积分。 */
    SILVER("银", 500, 25),
    /** 1000 至 1499 成长积分。 */
    GOLD("金", 1000, 20),
    /** 1500 及以上成长积分。 */
    DIAMOND("钻石", 1500, 15);

    private final String displayName;
    private final int growthThreshold;
    private final int pointsPerYuan;

    MemberLevel(String displayName, int growthThreshold, int pointsPerYuan) {
        this.displayName = displayName;
        this.growthThreshold = growthThreshold;
        this.pointsPerYuan = pointsPerYuan;
    }

    public String getDisplayName() { return displayName; }
    public int getGrowthThreshold() { return growthThreshold; }
    public int getPointsPerYuan() { return pointsPerYuan; }
}
