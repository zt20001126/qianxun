package com.qianxun.homestay.modules.creator.vo;

import java.time.OffsetDateTime;

/** D01-D05 达人响应。 */
public record CreatorVO(
        Long id,
        String name,
        Long channelId,
        Boolean enabled,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
