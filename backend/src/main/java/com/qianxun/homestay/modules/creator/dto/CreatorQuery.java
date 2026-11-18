package com.qianxun.homestay.modules.creator.dto;

/** D01 达人列表筛选条件。 */
public record CreatorQuery(
        /** 按所属渠道编号筛选；为空时不限制渠道。 */
        Long channelId,
        /** 按启用状态筛选；为空时不限制状态。 */
        Boolean enabled,
        /** 按达人名称模糊筛选。 */
        String name) {
}
