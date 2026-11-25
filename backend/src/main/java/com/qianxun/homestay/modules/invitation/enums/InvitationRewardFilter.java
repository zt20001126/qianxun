package com.qianxun.homestay.modules.invitation.enums;

/** 后台邀请奖励状态筛选，区分无奖励和待发放。 */
public enum InvitationRewardFilter {
    /** 全部邀请关系。 */ ALL,
    /** 尚未产生奖励。 */ NO_REWARD,
    /** 已产生奖励但尚未发放。 */ UNISSUED,
    /** 已登记发放。 */ ISSUED
}
