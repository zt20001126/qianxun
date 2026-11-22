package com.qianxun.homestay.modules.mall.dto;

import com.qianxun.homestay.modules.mall.enums.MallOrderStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/** 商城后台订单列表的筛选与分页条件。 */
public record MallOrderQueryRequest(
        /** 按商城订单状态筛选；为空时查询全部状态。 */
        MallOrderStatus status,
        /** 按订单号、商品名称、收货人或收货手机号模糊查询。 */
        @Size(max = 120) String keyword,
        /** 查询页码，从 1 开始。 */
        @Min(1) int page,
        /** 每页记录数，最大为 100。 */
        @Min(1) @Max(100) int size) {
}
