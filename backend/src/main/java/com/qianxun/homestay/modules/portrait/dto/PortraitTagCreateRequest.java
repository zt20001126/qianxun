package com.qianxun.homestay.modules.portrait.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 管理员新增可复用画像标签的请求。 */
public record PortraitTagCreateRequest(
        /** 标签名称。 */
        @NotBlank(message = "标签名称不能为空")
        @Size(max = 40, message = "标签名称不能超过 40 个字符")
        String name) {
}
