package com.qianxun.homestay.modules.mall.vo;

import java.util.List;

/** 商城商品分页结果。 */
public record MallProductPageVO(
        /** 当前页商品。 */ List<MallProductVO> list,
        /** 总商品数。 */ long total,
        /** 页码，从一开始。 */ int page,
        /** 每页商品数量。 */ int size) {
}
