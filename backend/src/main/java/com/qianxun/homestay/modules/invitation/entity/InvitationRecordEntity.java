package com.qianxun.homestay.modules.invitation.entity;

import java.time.LocalDateTime;

/** 邀请记录查询结果，仅供服务端脱敏转换。 */
public record InvitationRecordEntity(String phone, LocalDateTime createdAt, boolean hasCompletedOrder) { }
