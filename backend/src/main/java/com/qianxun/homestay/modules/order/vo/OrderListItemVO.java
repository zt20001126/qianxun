package com.qianxun.homestay.modules.order.vo;

import com.qianxun.homestay.modules.order.enums.OrderStatus;
import java.time.LocalDate;

/** 后台订单列表行及金额快照。 */
public record OrderListItemVO(
        /** 订单数据库编号。 */
        Long id,
        /** 系统生成的订单编号。 */
        String orderNo,
        /** 关联客房编号。 */
        Long roomId,
        /** 关联客房的展示房号。 */
        String roomNumber,
        /** 订单来源渠道编号。 */
        Long channelId,
        /** 订单关联达人编号；无达人时为空。 */
        Long creatorId,
        /** 预订联系人姓名。 */
        String guestName,
        /** 预订联系人手机号。 */
        String phone,
        /** 预定入住日期。 */
        LocalDate checkIn,
        /** 预定退房日期，不占用当日房晚。 */
        LocalDate checkOut,
        /** 住宿晚数。 */
        Integer nights,
        /** 当前订单状态。 */
        OrderStatus status,
        /** 订单实付金额快照，十进制字符串，单位为元。 */
        String paidAmount,
        /** 平台手续费快照，十进制字符串，单位为元。 */
        String platformFee,
        /** 平台或渠道提成快照，十进制字符串，单位为元。 */
        String platformCommission,
        /** 额外达人提成快照，十进制字符串，单位为元。 */
        String extraCommission,
        /** 有达人时的预期收入快照，十进制字符串，单位为元。 */
        String expectedAmount,
        /** 商家到手净额快照，十进制字符串，单位为元。 */
        String netAmount) {
}
