package com.qianxun.homestay.modules.order.entity;

import com.qianxun.homestay.modules.order.enums.OrderStatus;
import java.time.LocalDate;

/** orders 表的持久化映射。 */
public class OrderEntity {

    /** 订单数据库主键。 */
    private Long id;
    /** 系统生成的订单编号。 */
    private String orderNo;
    /** 第三方平台订单编号；小程序订单为空。 */
    private String platformOrderNo;
    /** 订单关联客房编号。 */
    private Long roomId;
    /** 订单来源渠道编号。 */
    private Long channelId;
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
    /** 预定入住日期，按北京时间的自然日记录。 */
    private LocalDate checkIn;
    /** 预定退房日期，不占用当日房晚。 */
    private LocalDate checkOut;
    /** 当前订单状态。 */
    private OrderStatus status;
    /** 订单备注；取消原因等补充说明保存在此字段。 */
    private String remark;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getPlatformOrderNo() { return platformOrderNo; }
    public void setPlatformOrderNo(String platformOrderNo) { this.platformOrderNo = platformOrderNo; }
    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public Long getChannelId() { return channelId; }
    public void setChannelId(Long channelId) { this.channelId = channelId; }
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
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
