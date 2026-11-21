package com.qianxun.homestay.modules.invitation.service;

import com.qianxun.homestay.modules.invitation.vo.InvitationPageVO;

/** 会员邀请标识及邀请关系业务。 */
public interface InvitationService {
    /** 获取或创建当前会员的长期邀请标识。 */
    String getOrCreateCode(Long memberId);
    /** 尝试为当前会员建立邀请关系，不覆盖已有关系。 */
    void bind(Long memberId, String code);
    /** 分页查询本人邀请记录及人数。 */
    InvitationPageVO list(Long memberId, int page, int size);
}
