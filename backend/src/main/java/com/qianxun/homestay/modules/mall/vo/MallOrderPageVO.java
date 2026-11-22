package com.qianxun.homestay.modules.mall.vo;

import java.util.List;

/** 后台商城订单分页结果。 */
public record MallOrderPageVO(
        /** 本页订单。 */ List<MallOrderVO> list,
        /** 总记录数。 */ long total,
        /** 页码。 */ int page,
        /** 每页数量。 */ int size) {
}
