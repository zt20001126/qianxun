package com.qianxun.homestay.modules.payment.domain;

import com.qianxun.homestay.modules.payment.vo.MockPayVO;

/** 模拟支付事务结果，用于提交超时关闭后再映射为 HTTP 冲突响应。 */
public record PaymentAttemptResult(boolean expired, MockPayVO payment) {
}
