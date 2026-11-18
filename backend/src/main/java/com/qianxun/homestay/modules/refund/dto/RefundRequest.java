package com.qianxun.homestay.modules.refund.dto;

import jakarta.validation.constraints.Size;

/** P03 自助取消退款请求。 */
public record RefundRequest(
        /** 顾客可选填写的取消原因。 */
        @Size(max = 300, message = "取消原因不能超过 300 个字符") String reason) {
}
