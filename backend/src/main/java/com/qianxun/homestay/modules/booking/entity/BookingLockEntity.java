package com.qianxun.homestay.modules.booking.entity;

import com.qianxun.homestay.modules.booking.enums.BookingLockStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** booking_lock 表的临时预订与锁房映射。 */
public class BookingLockEntity {

    /** 临时预订数据库主键。 */
    private Long id;
    /** 创建该临时预订的会员编号。 */
    private Long memberId;
    /** 被锁定的客房编号。 */
    private Long roomId;
    /** 预定入住日期。 */
    private LocalDate checkIn;
    /** 预定退房日期，不占用当天房晚。 */
    private LocalDate checkOut;
    /** 预订联系人姓名快照。 */
    private String guestName;
    /** 预订联系人手机号快照。 */
    private String phone;
    /** 入住人数快照。 */
    private Integer guestCount;
    /** 顾客填写的备注快照。 */
    private String remark;
    /** 创建交易时的房费原价快照。 */
    private BigDecimal roomAmount;
    /** 创建交易时的积分抵扣金额快照。 */
    private BigDecimal discountAmount;
    /** 当前交易预占的积分数量。 */
    private Integer pointsUsed;
    /** 积分抵扣后的待支付金额快照。 */
    private BigDecimal amount;
    /** 临时锁房状态。 */
    private BookingLockStatus status;
    /** 锁房失效时间，按北京时间存储。 */
    private LocalDateTime expiresAt;
    /** 临时预订创建时间。 */
    private LocalDateTime createdAt;
    /** 临时预订最近更新时间。 */
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }
    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public LocalDate getCheckIn() { return checkIn; }
    public void setCheckIn(LocalDate checkIn) { this.checkIn = checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
    public void setCheckOut(LocalDate checkOut) { this.checkOut = checkOut; }
    public String getGuestName() { return guestName; }
    public void setGuestName(String guestName) { this.guestName = guestName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Integer getGuestCount() { return guestCount; }
    public void setGuestCount(Integer guestCount) { this.guestCount = guestCount; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public BigDecimal getRoomAmount() { return roomAmount; }
    public void setRoomAmount(BigDecimal roomAmount) { this.roomAmount = roomAmount; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public Integer getPointsUsed() { return pointsUsed; }
    public void setPointsUsed(Integer pointsUsed) { this.pointsUsed = pointsUsed; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BookingLockStatus getStatus() { return status; }
    public void setStatus(BookingLockStatus status) { this.status = status; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
