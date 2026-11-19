package com.qianxun.homestay.modules.report.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 报表按月读取的订单及金额快照投影，不对应独立数据库表。 */
public class ReportOrderEntity {

    /** 订单唯一标识。 */
    private Long id;
    /** 房间唯一标识。 */
    private Long roomId;
    /** 房间号。 */
    private String roomNumber;
    /** 渠道唯一标识。 */
    private Long channelId;
    /** 渠道名称快照展示值。 */
    private String channelName;
    /** 达人唯一标识；无达人时为空。 */
    private Long creatorId;
    /** 达人名称。 */
    private String creatorName;
    /** 预订入住日期。 */
    private LocalDate checkIn;
    /** 预订退房日期，不包含退房当晚。 */
    private LocalDate checkOut;
    /** 当前订单状态编码。 */
    private String status;
    /** 顾客实付金额快照。 */
    private BigDecimal paidAmount;
    /** 平台手续费快照。 */
    private BigDecimal platformFee;
    /** 平台达人提成快照。 */
    private BigDecimal platformCommission;
    /** 达人额外提成快照。 */
    private BigDecimal extraCommission;
    /** 数据库生成的商家到手金额。 */
    private BigDecimal netAmount;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public String getCreatorName() { return creatorName; }
    public void setCreatorName(String creatorName) { this.creatorName = creatorName; }
    public LocalDate getCheckIn() { return checkIn; }
    public void setCheckIn(LocalDate checkIn) { this.checkIn = checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
    public void setCheckOut(LocalDate checkOut) { this.checkOut = checkOut; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }
    public BigDecimal getPlatformFee() { return platformFee; }
    public void setPlatformFee(BigDecimal platformFee) { this.platformFee = platformFee; }
    public BigDecimal getPlatformCommission() { return platformCommission; }
    public void setPlatformCommission(BigDecimal platformCommission) { this.platformCommission = platformCommission; }
    public BigDecimal getExtraCommission() { return extraCommission; }
    public void setExtraCommission(BigDecimal extraCommission) { this.extraCommission = extraCommission; }
    public BigDecimal getNetAmount() { return netAmount; }
    public void setNetAmount(BigDecimal netAmount) { this.netAmount = netAmount; }
}
