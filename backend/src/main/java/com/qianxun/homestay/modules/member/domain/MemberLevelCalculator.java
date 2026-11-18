package com.qianxun.homestay.modules.member.domain;

import com.qianxun.homestay.modules.member.enums.MemberLevel;
import java.util.List;
import org.springframework.stereotype.Component;

/** 根据累计成长积分动态计算会员等级与升级进度。 */
@Component
public class MemberLevelCalculator {

    private static final int POINTS_PER_LEVEL = 500;

    /** 返回成长积分对应的当前等级。 */
    public MemberLevel levelFor(Integer growthPoints) {
        int growth = Math.max(0, growthPoints == null ? 0 : growthPoints);
        if (growth >= MemberLevel.DIAMOND.getGrowthThreshold()) return MemberLevel.DIAMOND;
        if (growth >= MemberLevel.GOLD.getGrowthThreshold()) return MemberLevel.GOLD;
        if (growth >= MemberLevel.SILVER.getGrowthThreshold()) return MemberLevel.SILVER;
        return MemberLevel.ORDINARY;
    }

    /** 返回距离下一等级的成长积分；钻石等级返回零。 */
    public int pointsToNextLevel(Integer growthPoints) {
        int growth = Math.max(0, growthPoints == null ? 0 : growthPoints);
        MemberLevel level = levelFor(growth);
        if (level == MemberLevel.DIAMOND) return 0;
        return level.getGrowthThreshold() + POINTS_PER_LEVEL - growth;
    }

    /** 计算当前等级区间内的整数进度百分比；钻石等级固定为百分之百。 */
    public int progressPercent(Integer growthPoints) {
        int growth = Math.max(0, growthPoints == null ? 0 : growthPoints);
        MemberLevel level = levelFor(growth);
        if (level == MemberLevel.DIAMOND) return 100;
        int intoLevel = growth - level.getGrowthThreshold();
        return Math.min(100, intoLevel * 100 / POINTS_PER_LEVEL);
    }

    /** 返回接口规则页使用的四档等级定义。 */
    public List<MemberLevel> levels() {
        return List.of(MemberLevel.values());
    }
}
