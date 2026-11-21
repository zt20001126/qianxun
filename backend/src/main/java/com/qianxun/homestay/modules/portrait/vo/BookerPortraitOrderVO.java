package com.qianxun.homestay.modules.portrait.vo;

import java.time.LocalDate;

/** 用户画像列表中的单笔已完成订单信息。 */
public record BookerPortraitOrderVO(
        /** 订单数据库主键。 */
        Long orderId,
        /** 系统订单号。 */
        String orderNo,
        /** 预定入住日期。 */
        LocalDate checkIn,
        /** 订单实付金额，十进制字符串。 */
        String paidAmount,
        /** 预订人下单备注。 */
        String remark,
        /** 管理员录单备注。 */
        String adminRemark) {
}
