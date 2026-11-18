package com.qianxun.homestay.modules.payment.vo;

import java.time.OffsetDateTime;

/** P01 待支付交易与十分钟锁房响应。 */
public record PrepayVO(
        /** 临时预订编号。 */
        Long bookingId,
        /** 服务端生成的商户支付单号。 */
        String paymentNo,
        /** 房费原价快照，十进制字符串，单位为元。 */
        String roomAmount,
        /** 预占的积分数量。 */
        Integer pointsUsed,
        /** 积分抵扣金额快照，十进制字符串，单位为元。 */
        String discountAmount,
        /** 积分抵扣后的应付金额，十进制字符串，单位为元。 */
        String payAmount,
        /** 锁房过期时间，包含时区偏移。 */
        OffsetDateTime expiresAt) {
}
