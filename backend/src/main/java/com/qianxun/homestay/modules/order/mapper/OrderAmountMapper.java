package com.qianxun.homestay.modules.order.mapper;

import com.qianxun.homestay.modules.order.entity.OrderAmountEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 订单金额快照的数据库访问入口。 */
@Mapper
public interface OrderAmountMapper {

    int insert(OrderAmountEntity amount);

    OrderAmountEntity findByOrderId(@Param("orderId") Long orderId);
}
