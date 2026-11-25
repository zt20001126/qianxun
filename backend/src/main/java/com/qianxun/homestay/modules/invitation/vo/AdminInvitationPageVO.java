package com.qianxun.homestay.modules.invitation.vo;

import java.util.List;

/** 管理员邀请记录分页。 */
public record AdminInvitationPageVO(List<AdminInvitationRecordVO> list, long total, int page, int size) { }
