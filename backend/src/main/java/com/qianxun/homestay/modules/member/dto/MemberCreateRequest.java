package com.qianxun.homestay.modules.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** 后台预建会员请求；初始积分通过独立积分发放接口登记。 */
public record MemberCreateRequest(
        /** 中国大陆会员手机号，作为唯一会员标识。 */
        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入有效的中国大陆手机号")
        String phone) {
}
