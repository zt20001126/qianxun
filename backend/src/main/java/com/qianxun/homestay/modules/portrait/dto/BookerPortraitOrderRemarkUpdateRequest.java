package com.qianxun.homestay.modules.portrait.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** 更新一笔订单的用户备注和管理员备注。 */
public record BookerPortraitOrderRemarkUpdateRequest(
        /** 订单主键。 */
        @NotNull @Positive Long orderId,
        /** 预订人下单备注；空字符串表示清空。 */
        @Size(max = 500, message = "用户备注不能超过 500 个字符") String remark,
        /** 管理员备注；空字符串表示清空。 */
        @Size(max = 500, message = "管理员备注不能超过 500 个字符") String adminRemark) {
}
