package com.qianxun.homestay.modules.portrait.vo;

import java.time.LocalDate;

/** 画像编辑表单展示的订单备注；不包含可编辑金额或订单状态。 */
public record BookerPortraitEditableOrderVO(
        /** 订单数据库主键。 */
        Long orderId,
        /** 系统订单号。 */
        String orderNo,
        /** 预订入住日期。 */
        LocalDate checkIn,
        /** 订单状态编码。 */
        String status,
        /** 用户下单备注。 */
        String remark,
        /** 管理员备注。 */
        String adminRemark) {
}
