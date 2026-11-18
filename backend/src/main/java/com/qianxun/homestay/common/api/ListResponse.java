package com.qianxun.homestay.common.api;

import java.util.List;

/** 非分页列表响应，保持接口契约的 data.list 结构。 */
public record ListResponse<T>(List<T> list) {
}
