package com.qianxun.homestay.modules.room.dto;

import jakarta.validation.constraints.NotNull;

/** R09 客房上架状态请求。 */
public record RoomEnabledRequest(
        /** 客房是否上架并可供预订。 */
        @NotNull(message = "上架状态不能为空") Boolean enabled) {
}
