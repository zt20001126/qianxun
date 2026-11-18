package com.qianxun.homestay.modules.room.vo;

import java.math.BigDecimal;
import java.util.List;

/** R02 小程序房间详情。 */
public record AppRoomDetailVO(
        /** 客房数据库编号。 */
        Long id,
        /** 客房对外房号。 */
        String number,
        /** 客房名称。 */
        String name,
        /** 客房所在楼层。 */
        Integer floor,
        /** 客房每晚基础价格，十进制字符串，单位为元。 */
        String price,
        /** 客房允许入住的最大人数。 */
        Integer capacity,
        /** 客房床型说明。 */
        String bedInfo,
        /** 客房建筑面积，单位为平方米。 */
        BigDecimal area,
        /** 客房设施名称列表。 */
        List<String> facilities,
        /** 客房图片地址列表。 */
        List<String> images,
        /** 客房介绍。 */
        String description) {
}
