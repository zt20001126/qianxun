package com.qianxun.homestay.modules.availability.entity;

import java.time.LocalDate;

/** 房间在订单、封房或有效锁房中的一段占用日期。 */
public class AvailabilityRangeEntity {

    private LocalDate startDate;
    private LocalDate endDate;

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
}
