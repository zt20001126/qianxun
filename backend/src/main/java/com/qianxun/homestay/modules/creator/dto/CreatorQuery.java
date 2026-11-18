package com.qianxun.homestay.modules.creator.dto;

/** D01 达人列表筛选条件。 */
public record CreatorQuery(Long channelId, Boolean enabled, String name) {
}
