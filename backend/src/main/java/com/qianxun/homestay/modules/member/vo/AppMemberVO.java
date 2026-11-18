package com.qianxun.homestay.modules.member.vo;

/** 小程序会员中心资料，手机号仅展示脱敏结果。 */
public record AppMemberVO(
        /** 当前会员 ID。 */
        Long id,
        /** 脱敏手机号。 */
        String phone,
        /** 固定昵称。 */
        String nickname,
        /** 系统默认头像。 */
        String avatar,
        /** 累计成长积分。 */
        Integer growthPoints,
        /** 当前可用积分。 */
        Integer points,
        /** 当前动态计算的会员等级。 */
        String level,
        /** 升至下一等级还需的成长积分；最高等级为零。 */
        Integer nextLevelPoints,
        /** 当前等级区间进度百分比，最高等级固定为一百。 */
        Integer progressPercent) {
}
