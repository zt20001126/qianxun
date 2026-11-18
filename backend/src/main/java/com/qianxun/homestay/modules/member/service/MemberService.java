package com.qianxun.homestay.modules.member.service;

import com.qianxun.homestay.modules.member.dto.MemberCreateRequest;
import com.qianxun.homestay.modules.member.vo.AppMemberVO;
import com.qianxun.homestay.modules.member.vo.MemberCreatedVO;
import com.qianxun.homestay.modules.member.vo.MemberPageVO;

/** 提供会员中心资料与后台会员管理业务。 */
public interface MemberService {

    /** 按当前登录会员 ID 查询脱敏会员中心资料。 */
    AppMemberVO getMyMember(Long memberId);

    /** 按手机号筛选并分页查询后台会员列表。 */
    MemberPageVO findMembers(String phone, int page, int size);

    /** 按手机号预建会员，不直接写入积分余额。 */
    MemberCreatedVO createMember(MemberCreateRequest request);
}
