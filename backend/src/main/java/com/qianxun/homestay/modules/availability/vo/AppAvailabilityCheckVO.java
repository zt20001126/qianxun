package com.qianxun.homestay.modules.availability.vo;

/** R04 日期区间可订复核结果。 */
public record AppAvailabilityCheckVO(
        /** 指定住宿日期区间是否可预订。 */
        boolean available,
        /** 住宿晚数。 */
        int nights,
        /** 按客房基础夜价计算的房费，十进制字符串，单位为元。 */
        String roomAmount) {
}
