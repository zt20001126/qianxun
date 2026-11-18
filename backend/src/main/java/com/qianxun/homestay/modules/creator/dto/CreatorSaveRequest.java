package com.qianxun.homestay.modules.creator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** D03/D04 达人资料请求。 */
public record CreatorSaveRequest(
        /** 达人名称。 */
        @NotBlank(message = "达人名称不能为空") @Size(max = 80, message = "达人名称不能超过 80 个字符") String name,
        /** 达人所属的渠道数据库编号。 */
        @NotNull(message = "所属渠道不能为空") @Positive(message = "所属渠道编号不正确") Long channelId) {
}
