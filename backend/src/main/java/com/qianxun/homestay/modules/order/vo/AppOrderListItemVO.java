package com.qianxun.homestay.modules.order.vo;

import com.qianxun.homestay.modules.order.enums.OrderStatus;
import java.time.LocalDate;

/** 小程序本人订单列表项，只暴露顾客查看所需信息。 */
public record AppOrderListItemVO(
        /** 订单数据库编号。 */
        Long id,
        /** 系统生成的订单号。 */
        String orderNo,
        /** 关联客房编号。 */
        Long roomId,
        /** 客房展示房号。 */
        String roomNumber,
        /** 订单来源渠道编号。 */
        Long channelId,
        /** 关联达人编号；小程序订单为空。 */
        Long creatorId,
        /** 预订联系人姓名。 */
        String guestName,
        /** 预订联系人手机号。 */
        String phone,
        /** 入住日期。 */
        LocalDate checkIn,
        /** 退房日期，不占用当天房晚。 */
        LocalDate checkOut,
        /** 住宿晚数。 */
        Integer nights,
        /** 当前订单状态。 */
        OrderStatus status,
        /** 实付金额快照，单位为元的十进制字符串。 */
        String paidAmount) {
}
