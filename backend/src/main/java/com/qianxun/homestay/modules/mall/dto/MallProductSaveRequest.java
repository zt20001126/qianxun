package com.qianxun.homestay.modules.mall.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/** 后台创建或修改商品的请求。 */
public record MallProductSaveRequest(
        /** 商品名称。 */ @NotBlank @Size(max = 120) String name,
        /** 商品介绍及规格。 */ @NotBlank @Size(max = 10000) String description,
        /** 公开可读取的图片地址。 */ @NotNull @Size(max = 50) List<@NotBlank @Size(max = 2048) String> images,
        /** 单价，单位元。 */ @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal price) {
}
