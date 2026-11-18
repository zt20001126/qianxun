package com.qianxun.homestay.modules.order.vo;

import java.util.List;

/** 小程序本人订单分页数据。 */
public record AppOrderPageVO(
        /** 当前页订单列表。 */
        List<AppOrderListItemVO> list,
        /** 符合筛选条件的订单总数。 */
        long total,
        /** 当前页码，从 1 开始。 */
        int page,
        /** 每页记录数。 */
        int size) {
}
