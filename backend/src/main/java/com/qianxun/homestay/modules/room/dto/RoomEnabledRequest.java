package com.qianxun.homestay.modules.room.dto;

import jakarta.validation.constraints.NotNull;

/** R09 客房上架状态请求。 */
public record RoomEnabledRequest(@NotNull(message = "上架状态不能为空") Boolean enabled) {
}
