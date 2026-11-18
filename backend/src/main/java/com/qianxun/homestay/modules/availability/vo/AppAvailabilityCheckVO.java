package com.qianxun.homestay.modules.availability.vo;

/** R04 日期区间可订复核结果。 */
public record AppAvailabilityCheckVO(boolean available, int nights, String roomAmount) {
}
