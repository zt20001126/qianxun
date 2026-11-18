package com.qianxun.homestay.modules.order.service;

import com.qianxun.homestay.modules.order.dto.OrderQueryRequest;
import com.qianxun.homestay.modules.order.vo.OrderDetailVO;
import com.qianxun.homestay.modules.order.vo.OrderPageVO;

/** 后台订单读取服务。 */
public interface OrderQueryService {

    /** 按筛选条件分页查询订单。 */
    OrderPageVO findOrders(OrderQueryRequest query);

    /** 查询订单详情；订单不存在时返回 404。 */
    OrderDetailVO findOrder(Long orderId);
}
