package com.qianxun.homestay.modules.portrait.vo;

import java.util.List;

/** 管理端顾客画像列表的一行数据，含完整手机号和已完成订单汇总。 */
public record BookerPortraitListItemVO(
        /** 顾客画像主键。 */
        Long id,
        /** 预订人姓名；资料缺失时为空。 */
        String guestName,
        /** 预订人完整手机号；资料缺失时为空。 */
        String phone,
        /** 画像年龄；资料缺失时为空。 */
        Integer age,
        /** 固定群体分类编码；资料缺失时为空。 */
        String partyType,
        /** 手工填写或身份证号本地推算地区；资料缺失时为空。 */
        String inferredRegion,
        /** 该手机号是否存在于会员表。 */
        boolean member,
        /** 当前手机号画像的可编辑标签名称列表。 */
        List<String> tags,
        /** 可持续编辑的手机号级管理员备注。 */
        String adminRemark,
        /** 已完成订单数量。 */
        long completedOrderCount,
        /** 已完成订单累计实付金额，十进制字符串。 */
        String cumulativePaidAmount,
        /** 已完成订单列表，含每笔实付金额和两类备注。 */
        List<BookerPortraitOrderVO> completedOrders,
        /** 所有关联订单的备注，供持续编辑；不限制订单状态。 */
        List<BookerPortraitEditableOrderVO> orders) {
}
