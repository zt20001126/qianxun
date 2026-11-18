package com.qianxun.homestay.modules.creator.vo;

import java.time.OffsetDateTime;

/** D01-D05 达人响应。 */
public record CreatorVO(
        /** 达人数据库编号。 */
        Long id,
        /** 达人名称。 */
        String name,
        /** 达人所属渠道编号。 */
        Long channelId,
        /** 达人是否启用。 */
        Boolean enabled,
        /** 达人记录创建时间，包含时区偏移。 */
        OffsetDateTime createdAt,
        /** 达人记录最近更新时间，包含时区偏移。 */
        OffsetDateTime updatedAt) {
}
