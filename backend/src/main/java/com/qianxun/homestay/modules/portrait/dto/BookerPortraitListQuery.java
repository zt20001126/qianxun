package com.qianxun.homestay.modules.portrait.dto;

/** 管理端顾客画像列表的分页和姓名、手机号筛选条件。 */
public record BookerPortraitListQuery(
        /** 预订人姓名模糊筛选。 */
        String guestName,
        /** 预订人手机号模糊筛选。 */
        String phone,
        /** 从 1 开始的页码。 */
        int page,
        /** 单页记录数。 */
        int size) {
}
