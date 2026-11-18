package com.qianxun.homestay.modules.creator.dto;

import jakarta.validation.constraints.NotNull;

/** D05 达人启停请求。 */
public record CreatorEnabledRequest(
        /** 达人是否启用。 */
        @NotNull(message = "启用状态不能为空") Boolean enabled) {
}
