package com.qianxun.homestay.modules.invitation.vo;

import java.time.OffsetDateTime;

/** 脱敏的被邀请人及绑定时间。 */
public record InvitationRecordVO(String maskedPhone, OffsetDateTime boundAt, boolean hasCompletedOrder) { }
