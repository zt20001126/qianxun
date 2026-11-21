package com.qianxun.homestay.modules.invitation.vo;

import java.util.List;

/** 当前会员的邀请记录及总人数。 */
public record InvitationPageVO(List<InvitationRecordVO> list, long total, int page, int size) { }
