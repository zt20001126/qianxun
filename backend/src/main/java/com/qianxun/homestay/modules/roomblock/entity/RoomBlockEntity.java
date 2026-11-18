package com.qianxun.homestay.modules.roomblock.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 管理员主动封闭指定房间夜晚的持久化记录。 */
public class RoomBlockEntity {

    /** 封房记录数据库主键。 */
    private Long id;
    /** 被封闭的客房编号。 */
    private Long roomId;
    /** 封房开始日期，包含当天。 */
    private LocalDate startDate;
    /** 封房结束日期，不包含当天。 */
    private LocalDate endDate;
    /** 管理员填写的封房原因。 */
    private String reason;
    /** 封房记录创建时间。 */
    private LocalDateTime createdAt;
    /** 封房记录最近更新时间。 */
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
