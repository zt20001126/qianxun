package com.qianxun.homestay.modules.room.vo;

import java.util.List;

/** R01 小程序房间列表项。 */
public record AppRoomSummaryVO(
        Long id,
        String number,
        String name,
        String price,
        Integer capacity,
        String bedInfo,
        List<String> images) {
}
