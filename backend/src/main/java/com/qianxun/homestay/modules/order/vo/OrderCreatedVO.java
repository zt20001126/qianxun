package com.qianxun.homestay.modules.order.vo;

/** 手工录入第三方订单后的简要结果。 */
public record OrderCreatedVO(
        /** 新建订单的数据库编号。 */
        Long id,
        /** 新建订单的系统订单编号。 */
        String orderNo,
        /** 商家到手净额快照，十进制字符串，单位为元。 */
        String netAmount) {
}
