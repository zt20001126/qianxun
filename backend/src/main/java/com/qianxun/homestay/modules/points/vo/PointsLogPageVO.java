package com.qianxun.homestay.modules.points.vo;

import java.util.List;

/** 小程序或后台积分流水分页结果。 */
public record PointsLogPageVO(List<PointsLogListItemVO> list, long total, int page, int size) {
}
