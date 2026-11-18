package com.qianxun.homestay.modules.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** 第三方订单的创建时金额快照输入，金额使用十进制字符串。 */
public record OrderAmountRequest(
        /** 订单实付金额，使用十进制字符串，单位为元。 */
        @NotBlank(message = "实付金额不能为空") @Pattern(regexp = "^\\d{1,10}(\\.\\d{1,2})?$", message = "金额格式不正确")
                String paidAmount,
        /** 平台手续费，使用十进制字符串，单位为元。 */
        @Pattern(regexp = "^\\d{1,10}(\\.\\d{1,2})?$", message = "金额格式不正确") String platformFee,
        /** 平台或渠道提成，使用十进制字符串，单位为元。 */
        @Pattern(regexp = "^\\d{1,10}(\\.\\d{1,2})?$", message = "金额格式不正确") String platformCommission,
        /** 有达人时必填的预期收入，使用十进制字符串，单位为元。 */
        @Pattern(regexp = "^\\d{1,10}(\\.\\d{1,2})?$", message = "金额格式不正确") String expectedAmount) {
}
