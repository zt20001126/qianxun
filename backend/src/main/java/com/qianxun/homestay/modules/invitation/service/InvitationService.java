package com.qianxun.homestay.modules.invitation.service;

import com.qianxun.homestay.modules.invitation.vo.InvitationPageVO;
import com.qianxun.homestay.modules.invitation.enums.InvitationRewardFilter;
import com.qianxun.homestay.modules.invitation.vo.AdminInvitationPageVO;

/** 会员邀请标识及邀请关系业务。 */
public interface InvitationService {
    /** 获取或创建当前会员的长期邀请标识。 */
    String getOrCreateCode(Long memberId);
    /** 尝试为当前会员建立邀请关系，不覆盖已有关系。 */
    void bind(Long memberId, String code);
    /** 分页查询本人邀请记录及人数。 */
    InvitationPageVO list(Long memberId, int page, int size);
    /** 在订单状态变更前串行化同一会员的完成操作。 */
    void lockRewardMember(Long memberId);
    /** 在订单完成事务内记录符合条件的首单奖励。 */
    void recordReward(Long memberId, Long orderId);
    /** 管理员分页查看邀请关系及奖励。 */
    AdminInvitationPageVO adminList(int page, int size, String phone, InvitationRewardFilter rewardStatus);
    /** 管理员登记系统外发放状态。 */
    void setIssued(Long id, boolean issued);
}
