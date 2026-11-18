package com.qianxun.homestay.modules.admin.service;

import com.qianxun.homestay.modules.member.vo.MemberDetailVO;

/** 提供后台会员详情的跨业务数据聚合查询。 */
public interface AdminMemberDetailService {

    /** 按会员 ID 查询会员资料、关联订单及近期积分流水。 */
    MemberDetailVO getMemberDetail(Long memberId);
}
