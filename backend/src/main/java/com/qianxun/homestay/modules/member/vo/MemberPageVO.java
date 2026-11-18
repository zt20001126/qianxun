package com.qianxun.homestay.modules.member.vo;

import java.util.List;

/** 后台会员分页查询结果。 */
public record MemberPageVO(
        /** 当前页会员列表。 */
        List<MemberListItemVO> list,
        /** 匹配手机号条件的会员总数。 */
        long total,
        /** 当前页码，从一开始。 */
        int page,
        /** 每页条数。 */
        int size) {
}
