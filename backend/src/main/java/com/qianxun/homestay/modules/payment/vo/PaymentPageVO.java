package com.qianxun.homestay.modules.payment.vo;

import java.util.List;

/** P04 后台支付流水分页结果。 */
public record PaymentPageVO(List<PaymentListItemVO> list, long total, int page, int size) {
}
