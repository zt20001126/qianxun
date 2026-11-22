package com.qianxun.homestay.modules.mall.vo;

import java.time.OffsetDateTime;

/** 后台与订单所属会员查看的商城订单资料。 */
public record MallOrderVO(
        /** 订单编号。 */ Long id,
        /** 对外订单号。 */ String orderNo,
        /** 下单会员编号。 */ Long memberId,
        /** 商品名称快照。 */ String productName,
        /** 商品数量。 */ Integer quantity,
        /** 商品总额，单位元。 */ String totalAmount,
        /** 实付金额，单位元。 */ String paidAmount,
        /** 使用的积分。 */ Integer pointsUsed,
        /** 收货人。 */ String recipientName,
        /** 收货手机。 */ String recipientPhone,
        /** 收货地址。 */ String shippingAddress,
        /** 订单状态。 */ String status,
        /** 快递公司。 */ String expressCompany,
        /** 快递单号。 */ String trackingNo,
        /** 支付时间。 */ OffsetDateTime paidAt,
        /** 发货时间。 */ OffsetDateTime shippedAt,
        /** 完成时间。 */ OffsetDateTime completedAt,
        /** 下单时间。 */ OffsetDateTime createdAt) {
}
