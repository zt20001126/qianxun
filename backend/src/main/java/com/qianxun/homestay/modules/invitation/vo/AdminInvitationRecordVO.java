package com.qianxun.homestay.modules.invitation.vo;

import java.time.OffsetDateTime;

/** 管理员查看的邀请人、订单及人工发放状态。 */
public record AdminInvitationRecordVO(
        /** 邀请关系主键。 */ Long id,
        /** 邀请人会员 ID。 */ Long inviterId,
        /** 邀请人手机号。 */ String inviterPhone,
        /** 被邀请人会员 ID。 */ Long inviteeId,
        /** 被邀请人手机号。 */ String inviteePhone,
        /** 带时区的绑定时间。 */ OffsetDateTime boundAt,
        /** 首个获奖订单编号。 */ String orderNo,
        /** 订单实付金额字符串。 */ String paidAmount,
        /** 邀请奖励金额字符串。 */ String rewardAmount,
        /** 是否登记发放。 */ boolean rewardIssued) { }
