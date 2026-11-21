package com.qianxun.homestay.modules.order.service;

import com.qianxun.homestay.modules.order.dto.CancelOrderRequest;
import com.qianxun.homestay.modules.order.dto.CreateExternalOrderRequest;
import com.qianxun.homestay.modules.order.dto.UpdateOrderRequest;
import com.qianxun.homestay.modules.order.enums.OrderAction;
import com.qianxun.homestay.modules.portrait.dto.BookerPortraitSaveRequest;
import com.qianxun.homestay.modules.order.vo.OrderCreatedVO;
import com.qianxun.homestay.modules.order.vo.OrderDetailVO;
import com.qianxun.homestay.modules.order.vo.OrderStatusVO;

/** 后台订单创建、资料编辑与状态动作服务。 */
public interface OrderCommandService {

    /** 创建第三方订单和一对一金额快照。 */
    OrderCreatedVO createExternalOrder(CreateExternalOrderRequest request);

    /** 更新订单允许编辑的基本资料和可调整日期。 */
    OrderDetailVO updateOrder(Long orderId, UpdateOrderRequest request);

    /** 执行确认、入住、退房或取消状态动作。 */
    OrderStatusVO performAction(Long orderId, OrderAction action, CancelOrderRequest request);

    /**
     * 办理入住并在同一事务中保存可选画像。
     *
     * @param orderId 订单主键
     * @param portraitRequest 可选画像内容
     * @return 更新后的订单状态
     */
    OrderStatusVO checkIn(Long orderId, BookerPortraitSaveRequest portraitRequest);
}
