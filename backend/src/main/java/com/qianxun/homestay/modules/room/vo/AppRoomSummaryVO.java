package com.qianxun.homestay.modules.room.vo;

import java.util.List;

/** R01 小程序房间列表项。 */
public record AppRoomSummaryVO(
        /** 客房数据库编号。 */
        Long id,
        /** 客房对外房号。 */
        String number,
        /** 客房名称。 */
        String name,
        /** 客房每晚基础价格，十进制字符串，单位为元。 */
        String price,
        /** 客房允许入住的最大人数。 */
        Integer capacity,
        /** 客房床型说明。 */
        String bedInfo,
        /** 客房图片地址列表。 */
        List<String> images,
        /** 查询日期区间内是否可预订。 */
        Boolean available) {
}
