package com.qianxun.homestay.modules.mall.vo;

import java.time.OffsetDateTime;

/** 小程序会员查看本人商城订单的详情视图，不暴露内部会员关联标识。 */
public record AppMallOrderDetailVO(
        /** 商城订单内部标识，仅用于后续退款等本人操作。 */ Long id,
        /** 对外展示的商城订单号。 */ String orderNo,
        /** 支付成功时保存的商品名称快照。 */ String productName,
        /** 购买数量。 */ Integer quantity,
        /** 商品原价总额，单位元。 */ String totalAmount,
        /** 积分抵扣后的实际支付金额，单位元。 */ String paidAmount,
        /** 本单实际抵扣的积分数量。 */ Integer pointsUsed,
        /** 支付成功时保存的收货人姓名。 */ String recipientName,
        /** 支付成功时保存的收货手机号。 */ String recipientPhone,
        /** 支付成功时保存的收货地址。 */ String shippingAddress,
        /** 商城订单状态。 */ String status,
        /** 商家填写的快递公司。 */ String expressCompany,
        /** 商家填写的快递单号。 */ String trackingNo,
        /** 支付完成时间。 */ OffsetDateTime paidAt,
        /** 商家发货时间。 */ OffsetDateTime shippedAt,
        /** 管理员完成订单时间。 */ OffsetDateTime completedAt,
        /** 商城订单创建时间。 */ OffsetDateTime createdAt) {
}
