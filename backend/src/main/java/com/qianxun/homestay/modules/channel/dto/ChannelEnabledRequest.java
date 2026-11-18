package com.qianxun.homestay.modules.channel.dto;

import jakarta.validation.constraints.NotNull;

/** C02 渠道启停请求。 */
public record ChannelEnabledRequest(@NotNull(message = "启用状态不能为空") Boolean enabled) {
}
