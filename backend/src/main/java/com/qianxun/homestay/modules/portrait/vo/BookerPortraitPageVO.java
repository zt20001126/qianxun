package com.qianxun.homestay.modules.portrait.vo;

import java.util.List;

/** 管理端顾客画像分页响应。 */
public record BookerPortraitPageVO(
        /** 当前页画像列表。 */
        List<BookerPortraitListItemVO> list,
        /** 符合筛选条件的画像总数。 */
        long total,
        /** 当前页码，从 1 开始。 */
        int page,
        /** 每页记录数。 */
        int size) {
}
