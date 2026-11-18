package com.qianxun.homestay.modules.auth.vo;

/**
 * A01/A02 返回的小程序当前会员资料，不包含 OpenID 等内部字段。
 */
public record MemberVO(
        /** 会员数据库 ID。 */
        Long id,
        /** 会员手机号。 */
        String phone,
        /** 由手机号后四位生成的固定昵称。 */
        String nickname,
        /** 系统统一默认头像地址。 */
        String avatar,
        /** 当前可用积分。 */
        Integer points,
        /** 累计成长积分。 */
        Integer growthPoints) {
}
