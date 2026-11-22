package com.qianxun.homestay.modules.mall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 管理员发货时提交的快递信息。 */
public record MallShipRequest(
        /** 快递公司名称。 */ @NotBlank @Size(max = 100) String expressCompany,
        /** 快递单号。 */ @NotBlank @Size(max = 100) String trackingNo) {
}
