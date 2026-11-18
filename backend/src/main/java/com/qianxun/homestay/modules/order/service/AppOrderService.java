package com.qianxun.homestay.modules.order.service;

import com.qianxun.homestay.modules.order.enums.OrderStatus;
import com.qianxun.homestay.modules.order.vo.AppOrderDetailVO;
import com.qianxun.homestay.modules.order.vo.AppOrderPageVO;

/** 提供小程序当前会员自己的正式订单查询。 */
public interface AppOrderService {

    /** 按状态分页查询当前会员订单。 */
    AppOrderPageVO findMyOrders(Long memberId, OrderStatus status, int page, int size);

    /** 查询当前会员的指定订单；不存在或不属于本人时返回 404。 */
    AppOrderDetailVO findMyOrder(Long memberId, Long orderId);
}
