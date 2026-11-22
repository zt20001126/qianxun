package com.qianxun.homestay.modules.notification.vo;

/** 每日房态邮件中一条客房安排的展示数据。 */
public class DailyRoomStatusRowVO {

    /** 客房对外房号。 */
    private final String roomNumber;
    /** 客房名称。 */
    private final String roomName;
    /** 当日房态动作，例如入住、续住、退房或空房。 */
    private final String roomStatus;
    /** 订单当前实际状态编码及中文名称。 */
    private final String orderStatus;
    /** 系统订单号。 */
    private final String orderNo;
    /** 订单来源渠道。 */
    private final String channelName;
    /** 预订联系人姓名。 */
    private final String guestName;
    /** 预订联系人电话。 */
    private final String phone;
    /** 入住人数。 */
    private final Integer guestCount;
    /** 预定入住日期。 */
    private final String checkIn;
    /** 预定退房日期。 */
    private final String checkOut;
    /** 订单总住宿晚数。 */
    private final Integer totalNights;
    /** 顾客填写的预订备注。 */
    private final String remark;
    /** 管理员内部备注。 */
    private final String adminRemark;

    public DailyRoomStatusRowVO(String roomNumber, String roomName, String roomStatus, String orderStatus,
            String orderNo, String channelName, String guestName, String phone, Integer guestCount,
            String checkIn, String checkOut, Integer totalNights, String remark, String adminRemark) {
        this.roomNumber = roomNumber;
        this.roomName = roomName;
        this.roomStatus = roomStatus;
        this.orderStatus = orderStatus;
        this.orderNo = orderNo;
        this.channelName = channelName;
        this.guestName = guestName;
        this.phone = phone;
        this.guestCount = guestCount;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.totalNights = totalNights;
        this.remark = remark;
        this.adminRemark = adminRemark;
    }

    public String getRoomNumber() { return roomNumber; }
    public String getRoomName() { return roomName; }
    public String getRoomStatus() { return roomStatus; }
    public String getOrderStatus() { return orderStatus; }
    public String getOrderNo() { return orderNo; }
    public String getChannelName() { return channelName; }
    public String getGuestName() { return guestName; }
    public String getPhone() { return phone; }
    public Integer getGuestCount() { return guestCount; }
    public String getCheckIn() { return checkIn; }
    public String getCheckOut() { return checkOut; }
    public Integer getTotalNights() { return totalNights; }
    public String getRemark() { return remark; }
    public String getAdminRemark() { return adminRemark; }
}
