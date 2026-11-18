package com.qianxun.homestay.modules.availability.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** 描述入住日包含、退房日不包含的住宿日期区间。 */
public record StayDateRange(LocalDate checkIn, LocalDate checkOut) {

    /** 返回住宿晚数。 */
    public long nights() {
        return ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    /** 判断两个住宿区间是否至少共用一个夜晚。 */
    public boolean overlaps(StayDateRange other) {
        return checkIn.isBefore(other.checkOut()) && checkOut.isAfter(other.checkIn());
    }
}
