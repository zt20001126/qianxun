package com.qianxun.homestay.modules.channel.vo;

import java.time.OffsetDateTime;

/** C01/C02 渠道响应。 */
public record ChannelVO(
        /** 渠道数据库编号。 */
        Long id,
        /** 渠道展示名称。 */
        String name,
        /** 渠道是否启用。 */
        Boolean enabled,
        /** 渠道记录创建时间，包含时区偏移。 */
        OffsetDateTime createdAt,
        /** 渠道记录最近更新时间，包含时区偏移。 */
        OffsetDateTime updatedAt) {
}
