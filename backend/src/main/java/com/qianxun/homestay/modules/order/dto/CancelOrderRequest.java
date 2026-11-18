package com.qianxun.homestay.modules.order.dto;

import jakarta.validation.constraints.Size;

/** 管理员取消第三方订单时可提供的原因。 */
public record CancelOrderRequest(
        /** 取消原因；服务端将其追加到订单备注中保存。 */
        @Size(max = 300, message = "取消原因不能超过 300 个字符") String reason) {
}
