package com.qianxun.homestay.modules.report.entity;

import java.time.LocalDate;

/** 指定日期房态总览的查询投影，不对应独立数据库表。 */
public class DashboardRoomEntity {

    /** 房间唯一标识。 */
    private Long roomId;
    /** 房号。 */
    private String roomNumber;
    /** 房间名称。 */
    private String roomName;
    /** 当日覆盖的订单唯一标识；无订单时为空。 */
    private Long orderId;
    /** 预订联系人姓名。 */
    private String guestName;
    /** 订单登记的预订入住人数。 */
    private Integer guestCount;
    /** 订单入住日期。 */
    private LocalDate checkIn;
    /** 订单退房日期。 */
    private LocalDate checkOut;
    /** 订单状态编码。 */
    private String orderStatus;
    /** 订单来源名称。 */
    private String channelName;
    /** 当日命中的管理员封房记录。 */
    private Long blockId;

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getGuestName() { return guestName; }
    public void setGuestName(String guestName) { this.guestName = guestName; }
    public Integer getGuestCount() { return guestCount; }
    public void setGuestCount(Integer guestCount) { this.guestCount = guestCount; }
    public LocalDate getCheckIn() { return checkIn; }
    public void setCheckIn(LocalDate checkIn) { this.checkIn = checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
    public void setCheckOut(LocalDate checkOut) { this.checkOut = checkOut; }
    public String getOrderStatus() { return orderStatus; }
    public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }
    public String getChannelName() { return channelName; }
    public void setChannelName(String channelName) { this.channelName = channelName; }
    public Long getBlockId() { return blockId; }
    public void setBlockId(Long blockId) { this.blockId = blockId; }
}
