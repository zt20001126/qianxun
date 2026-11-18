package com.qianxun.homestay.modules.room.vo;

import java.math.BigDecimal;
import java.util.List;

/** R02 小程序房间详情。 */
public record AppRoomDetailVO(
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
        String description) {
}
