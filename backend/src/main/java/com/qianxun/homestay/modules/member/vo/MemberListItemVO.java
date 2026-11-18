package com.qianxun.homestay.modules.member.vo;

import java.time.OffsetDateTime;

/** 后台会员列表展示数据。 */
public record MemberListItemVO(
        /** 会员 ID。 */
        Long id,
        /** 后台管理使用的完整会员手机号。 */
        String phone,
        /** 固定昵称。 */
        String nickname,
        /** 累计成长积分。 */
        Integer growthPoints,
        /** 当前可用积分。 */
        Integer points,
        /** 根据成长积分计算的会员等级。 */
        String level,
        /** 会员资料创建时间。 */
        OffsetDateTime createdAt) {
}
