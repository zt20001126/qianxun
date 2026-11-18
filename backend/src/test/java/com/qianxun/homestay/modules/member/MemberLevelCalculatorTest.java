package com.qianxun.homestay.modules.member;

import static org.assertj.core.api.Assertions.assertThat;

import com.qianxun.homestay.modules.member.domain.MemberLevelCalculator;
import com.qianxun.homestay.modules.member.enums.MemberLevel;
import org.junit.jupiter.api.Test;

/** 验证等级临界值、下一等级距离与进度计算。 */
class MemberLevelCalculatorTest {

    private final MemberLevelCalculator calculator = new MemberLevelCalculator();

    @Test
    void levelThresholdsAndProgressAreCalculatedFromGrowthPoints() {
        assertThat(calculator.levelFor(499)).isEqualTo(MemberLevel.ORDINARY);
        assertThat(calculator.levelFor(500)).isEqualTo(MemberLevel.SILVER);
        assertThat(calculator.levelFor(999)).isEqualTo(MemberLevel.SILVER);
        assertThat(calculator.levelFor(1000)).isEqualTo(MemberLevel.GOLD);
        assertThat(calculator.levelFor(1500)).isEqualTo(MemberLevel.DIAMOND);
        assertThat(calculator.pointsToNextLevel(499)).isEqualTo(1);
        assertThat(calculator.progressPercent(499)).isEqualTo(99);
        assertThat(calculator.pointsToNextLevel(1500)).isZero();
        assertThat(calculator.progressPercent(1500)).isEqualTo(100);
    }
}
