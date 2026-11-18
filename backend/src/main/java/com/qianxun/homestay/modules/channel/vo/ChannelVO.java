package com.qianxun.homestay.modules.channel.vo;

import java.time.OffsetDateTime;

/** C01/C02 渠道响应。 */
public record ChannelVO(Long id, String name, Boolean enabled, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
}
