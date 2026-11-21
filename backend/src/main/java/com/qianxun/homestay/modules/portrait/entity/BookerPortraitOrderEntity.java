package com.qianxun.homestay.modules.portrait.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 顾客画像列表中的单笔已完成订单及两类订单备注。 */
public class BookerPortraitOrderEntity {

    /** 所属顾客画像主键。 */
    private Long profileId;
    /** 订单数据库主键。 */
    private Long orderId;
    /** 系统订单号。 */
    private String orderNo;
    /** 预定入住日期。 */
    private LocalDate checkIn;
    /** 订单实付金额。 */
    private BigDecimal paidAmount;
    /** 当前订单状态编码。 */
    private String status;
    /** 预订人下单备注。 */
    private String remark;
    /** 管理员创建订单时填写的内部备注。 */
    private String adminRemark;

    public Long getProfileId() { return profileId; }
    public void setProfileId(Long profileId) { this.profileId = profileId; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public LocalDate getCheckIn() { return checkIn; }
    public void setCheckIn(LocalDate checkIn) { this.checkIn = checkIn; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getAdminRemark() { return adminRemark; }
    public void setAdminRemark(String adminRemark) { this.adminRemark = adminRemark; }
}
