package com.qianxun.homestay.modules.order.vo;

import java.util.List;

/** 后台分页订单数据。 */
public record OrderPageVO(
        /** 当前页的订单列表。 */
        List<OrderListItemVO> list,
        /** 符合筛选条件的订单总数。 */
        long total,
        /** 当前页码，从 1 开始。 */
        int page,
        /** 每页记录数。 */
        int size) {
}
