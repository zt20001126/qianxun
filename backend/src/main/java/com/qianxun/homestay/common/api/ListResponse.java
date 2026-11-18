package com.qianxun.homestay.common.api;

import java.util.List;

/** 非分页列表响应，保持接口契约的 data.list 结构。 */
/** 通用列表响应数据。 */
public record ListResponse<T>(
        /** 当前响应包含的列表数据。 */
        List<T> list) {
}
