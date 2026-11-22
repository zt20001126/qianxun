package com.qianxun.homestay.modules.mall.vo;

import java.time.OffsetDateTime;
import java.util.List;

/** 商城商品展示对象。 */
public record MallProductVO(
        /** 商品编号。 */ Long id,
        /** 商品名称。 */ String name,
        /** 商品介绍及规格。 */ String description,
        /** 商品图片地址。 */ List<String> images,
        /** 当前单价，单位元的十进制字符串。 */ String price,
        /** 上架状态。 */ String status,
        /** 创建时间。 */ OffsetDateTime createdAt,
        /** 更新时间。 */ OffsetDateTime updatedAt) {
}
