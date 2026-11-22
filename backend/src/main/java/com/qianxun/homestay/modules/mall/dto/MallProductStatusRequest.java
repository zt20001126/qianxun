package com.qianxun.homestay.modules.mall.dto;

import com.qianxun.homestay.modules.mall.enums.MallProductStatus;
import jakarta.validation.constraints.NotNull;

/** 管理员调整商品上架状态的请求。 */
public record MallProductStatusRequest(
        /** 要设置的上架状态。 */ @NotNull MallProductStatus status) {
}
