package com.qianxun.homestay.modules.order.vo;

import com.qianxun.homestay.modules.order.enums.OrderStatus;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/** 小程序本人订单详情，不包含内部渠道费用和佣金字段。 */
public record AppOrderDetailVO(
        /** 订单数据库编号。 */
        Long id,
        /** 系统生成的订单号。 */
        String orderNo,
        /** 关联客房编号。 */
        Long roomId,
        /** 客房展示房号。 */
        String roomNumber,
        /** 预订联系人姓名。 */
        String guestName,
        /** 预订联系人手机号。 */
        String phone,
        /** 入住人数。 */
        Integer guestCount,
        /** 入住日期。 */
        LocalDate checkIn,
        /** 退房日期，不占用当天房晚。 */
        LocalDate checkOut,
        /** 住宿晚数。 */
        Integer nights,
        /** 当前订单状态。 */
        OrderStatus status,
        /** 订单备注。 */
        String remark,
        /** 实付金额快照，单位为元的十进制字符串。 */
        String paidAmount,
        /** 订单创建时间，包含北京时间偏移。 */
        OffsetDateTime createdAt,
        /** 订单更新时间，包含北京时间偏移。 */
        OffsetDateTime updatedAt) {
}
