package com.qianxun.homestay.modules.room.vo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/** 后台客房资料响应。 */
public record RoomAdminVO(
        Long id,
        String number,
        String name,
        Integer floor,
        String price,
        Integer capacity,
        String bedInfo,
        BigDecimal area,
        List<String> facilities,
        List<String> images,
        String description,
        Boolean enabled,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
