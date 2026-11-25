package com.qianxun.homestay.modules.invitation.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 管理员邀请关系及订单查询结果。 */
public record AdminInvitationRecordEntity(
        /** 邀请关系主键。 */ Long id,
        /** 邀请人会员 ID。 */ Long inviterId,
        /** 邀请人手机号。 */ String inviterPhone,
        /** 被邀请人会员 ID。 */ Long inviteeId,
        /** 被邀请人手机号。 */ String inviteePhone,
        /** 绑定时间。 */ LocalDateTime boundAt,
        /** 首个获奖订单编号。 */ String orderNo,
        /** 订单实付金额。 */ BigDecimal paidAmount,
        /** 邀请奖励金额。 */ BigDecimal rewardAmount,
        /** 是否登记发放。 */ boolean rewardIssued) { }
