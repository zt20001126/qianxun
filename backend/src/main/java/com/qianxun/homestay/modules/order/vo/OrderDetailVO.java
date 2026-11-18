package com.qianxun.homestay.modules.order.vo;

import com.qianxun.homestay.modules.order.enums.OrderStatus;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/** 后台订单详情及创建时不可变金额快照。 */
public record OrderDetailVO(
        /** 订单数据库编号。 */
        Long id,
        /** 系统生成的订单编号。 */
        String orderNo,
        /** 第三方平台订单编号；小程序订单为空。 */
        String platformOrderNo,
        /** 关联客房编号。 */
        Long roomId,
        /** 关联客房的展示房号。 */
        String roomNumber,
        /** 订单来源渠道编号。 */
        Long channelId,
        /** 订单来源渠道名称。 */
        String channelName,
        /** 订单关联达人编号；无达人时为空。 */
        Long creatorId,
        /** 小程序下单会员编号；第三方订单为空。 */
        Long memberId,
        /** 预订联系人姓名。 */
        String guestName,
        /** 预订联系人手机号。 */
        String phone,
        /** 入住人数。 */
        Integer guestCount,
        /** 预定入住日期。 */
        LocalDate checkIn,
        /** 预定退房日期，不占用当日房晚。 */
        LocalDate checkOut,
        /** 住宿晚数。 */
        Integer nights,
        /** 当前订单状态。 */
        OrderStatus status,
        /** 订单备注，可能包含管理员追加的取消原因。 */
        String remark,
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
        /** 数据库生成的商家到手净额快照，十进制字符串，单位为元。 */
        String netAmount,
        /** 订单创建时间，包含时区偏移。 */
        OffsetDateTime createdAt,
        /** 订单最近更新时间，包含时区偏移。 */
        OffsetDateTime updatedAt) {
}
