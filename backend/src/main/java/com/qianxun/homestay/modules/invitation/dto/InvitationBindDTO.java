package com.qianxun.homestay.modules.invitation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** 邀请卡片携带的公开标识。 */
public record InvitationBindDTO(@NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{22}") String inviteCode) { }
