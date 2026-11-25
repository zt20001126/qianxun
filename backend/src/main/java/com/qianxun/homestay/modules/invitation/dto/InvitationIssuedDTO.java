package com.qianxun.homestay.modules.invitation.dto;

import jakarta.validation.constraints.NotNull;

/** 管理员登记奖励是否已在系统外发放。 */
public record InvitationIssuedDTO(@NotNull Boolean rewardIssued) { }
