package com.qianxun.homestay.modules.report.entity;

import java.time.LocalDate;

/** 报表中用于扣除管理员封房间夜的区间投影。 */
public class ReportBlockEntity {

    /** 被封房间的唯一标识。 */
    private Long roomId;
    /** 首个封房住宿夜晚。 */
    private LocalDate startDate;
    /** 封房结束日期，不包含当天。 */
    private LocalDate endDate;

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
}
