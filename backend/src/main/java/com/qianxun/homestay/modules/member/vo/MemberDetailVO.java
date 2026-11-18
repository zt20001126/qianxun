package com.qianxun.homestay.modules.member.vo;

import java.time.OffsetDateTime;
import java.util.List;

/** 后台会员详情资料，不包含预留 OpenID。 */
public record MemberDetailVO(
        /** 会员 ID。 */
        Long id,
        /** 会员手机号。 */
        String phone,
        /** 固定昵称。 */
        String nickname,
        /** 系统默认头像地址。 */
        String avatar,
        /** 累计成长积分。 */
        Integer growthPoints,
        /** 当前可用积分。 */
        Integer points,
        /** 根据成长积分计算的会员等级。 */
        String level,
        /** 会员资料创建时间。 */
        OffsetDateTime createdAt,
        /** 按会员 ID 关联的消费订单摘要。 */
        List<MemberOrderVO> orders,
        /** 最近一页积分流水，完整历史可使用 T04 继续分页。 */
        List<MemberPointsLogVO> pointsLogs,
        /** 当前会员积分流水总数。 */
        long pointsLogTotal) {
}
