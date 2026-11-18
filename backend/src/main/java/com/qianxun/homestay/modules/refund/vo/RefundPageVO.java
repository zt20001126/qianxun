package com.qianxun.homestay.modules.refund.vo;

import java.util.List;

/** P05 后台退款流水分页结果。 */
public record RefundPageVO(List<RefundListItemVO> list, long total, int page, int size) {
}
