package com.qianxun.homestay.modules.order.entity;

import com.qianxun.homestay.modules.order.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 订单列表及详情查询使用的关联结果映射。 */
public class OrderViewEntity {

    /** 订单数据库主键。 */
    private Long id;
    /** 系统生成的订单编号。 */
    private String orderNo;
    /** 第三方平台订单编号；小程序订单为空。 */
    private String platformOrderNo;
    /** 订单关联客房编号。 */
    private Long roomId;
    /** 关联客房的展示房号。 */
    private String roomNumber;
    /** 订单来源渠道编号。 */
    private Long channelId;
    /** 订单来源渠道名称。 */
    private String channelName;
    /** 订单关联达人编号；无达人时为空。 */
    private Long creatorId;
    /** 小程序下单会员编号；第三方订单为空。 */
    private Long memberId;
    /** 预订联系人姓名。 */
    private String guestName;
    /** 预订联系人手机号。 */
    private String phone;
    /** 入住人数。 */
    private Integer guestCount;
    /** 预定入住日期。 */
    private LocalDate checkIn;
    /** 预定退房日期，不占用当日房晚。 */
    private LocalDate checkOut;
    /** 住宿晚数。 */
    private Integer nights;
    /** 当前订单状态。 */
    private OrderStatus status;
    /** 订单备注。 */
    private String remark;
    /** 管理员在创建订单时填写的内部备注。 */
    private String adminRemark;
    /** 订单实付金额快照，单位为元。 */
    private BigDecimal paidAmount;
    /** 积分抵扣前房费总额，由实付金额与抵扣流水还原。 */
    private BigDecimal roomAmount;
    /** 该订单抵扣的积分数量。 */
    private Integer pointsUsed;
    /** 订单使用积分抵扣的金额，单位为元。 */
    private BigDecimal discountAmount;
    /** 平台手续费快照，单位为元。 */
    private BigDecimal platformFee;
    /** 平台或渠道提成快照，单位为元。 */
    private BigDecimal platformCommission;
    /** 额外达人提成快照，单位为元。 */
    private BigDecimal extraCommission;
    /** 有达人时记录的预期收入快照，单位为元。 */
    private BigDecimal expectedAmount;
    /** 数据库生成的商家到手净额，单位为元。 */
    private BigDecimal netAmount;
    /** 订单创建时间。 */
    private LocalDateTime createdAt;
    /** 订单最近更新时间。 */
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getPlatformOrderNo() { return platformOrderNo; }
    public void setPlatformOrderNo(String platformOrderNo) { this.platformOrderNo = platformOrderNo; }
    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
    public Long getChannelId() { return channelId; }
    public void setChannelId(Long channelId) { this.channelId = channelId; }
    public String getChannelName() { return channelName; }
    public void setChannelName(String channelName) { this.channelName = channelName; }
    public Long getCreatorId() { return creatorId; }
    public void setCreatorId(Long creatorId) { this.creatorId = creatorId; }
    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }
    public String getGuestName() { return guestName; }
    public void setGuestName(String guestName) { this.guestName = guestName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Integer getGuestCount() { return guestCount; }
    public void setGuestCount(Integer guestCount) { this.guestCount = guestCount; }
    public LocalDate getCheckIn() { return checkIn; }
    public void setCheckIn(LocalDate checkIn) { this.checkIn = checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
    public void setCheckOut(LocalDate checkOut) { this.checkOut = checkOut; }
    public Integer getNights() { return nights; }
    public void setNights(Integer nights) { this.nights = nights; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getAdminRemark() { return adminRemark; }
    public void setAdminRemark(String adminRemark) { this.adminRemark = adminRemark; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }
    public BigDecimal getRoomAmount() { return roomAmount; }
    public void setRoomAmount(BigDecimal roomAmount) { this.roomAmount = roomAmount; }
    public Integer getPointsUsed() { return pointsUsed; }
    public void setPointsUsed(Integer pointsUsed) { this.pointsUsed = pointsUsed; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public BigDecimal getPlatformFee() { return platformFee; }
    public void setPlatformFee(BigDecimal platformFee) { this.platformFee = platformFee; }
    public BigDecimal getPlatformCommission() { return platformCommission; }
    public void setPlatformCommission(BigDecimal platformCommission) { this.platformCommission = platformCommission; }
    public BigDecimal getExtraCommission() { return extraCommission; }
    public void setExtraCommission(BigDecimal extraCommission) { this.extraCommission = extraCommission; }
    public BigDecimal getExpectedAmount() { return expectedAmount; }
    public void setExpectedAmount(BigDecimal expectedAmount) { this.expectedAmount = expectedAmount; }
    public BigDecimal getNetAmount() { return netAmount; }
    public void setNetAmount(BigDecimal netAmount) { this.netAmount = netAmount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
