package com.qianxun.homestay.modules.mall.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 商城订单数据库字段映射。 */
public class MallOrderEntity {
    /** 订单编号。 */
    private Long id;
    /** 订单号。 */
    private String orderNo;
    /** 下单会员。 */
    private Long memberId;
    /** 商品编号。 */
    private Long productId;
    /** 商品名称快照。 */
    private String productName;
    /** 当前商品第一张图片地址，仅用于订单列表展示，不写入订单快照。 */
    private String productImage;
    /** 购买数量。 */
    private Integer quantity;
    /** 商品总额。 */
    private BigDecimal totalAmount;
    /** 实付金额。 */
    private BigDecimal paidAmount;
    /** 抵扣积分。 */
    private Integer pointsUsed;
    /** 收货人。 */
    private String recipientName;
    /** 收货手机。 */
    private String recipientPhone;
    /** 收货地址。 */
    private String shippingAddress;
    /** 订单状态。 */
    private String status;
    /** 快递公司。 */
    private String expressCompany;
    /** 快递单号。 */
    private String trackingNo;
    /** 支付时间。 */
    private LocalDateTime paidAt;
    /** 发货时间。 */
    private LocalDateTime shippedAt;
    /** 完成时间。 */
    private LocalDateTime completedAt;
    /** 创建时间。 */
    private LocalDateTime createdAt;
    /** 更新时间。 */
    private LocalDateTime updatedAt;
    public Long getId() { return id; }
    public void setId(Long value) { this.id = value; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String value) { this.orderNo = value; }
    public Long getMemberId() { return memberId; }
    public void setMemberId(Long value) { this.memberId = value; }
    public Long getProductId() { return productId; }
    public void setProductId(Long value) { this.productId = value; }
    public String getProductName() { return productName; }
    public void setProductName(String value) { this.productName = value; }
    public String getProductImage() { return productImage; }
    public void setProductImage(String value) { this.productImage = value; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer value) { this.quantity = value; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal value) { this.totalAmount = value; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal value) { this.paidAmount = value; }
    public Integer getPointsUsed() { return pointsUsed; }
    public void setPointsUsed(Integer value) { this.pointsUsed = value; }
    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String value) { this.recipientName = value; }
    public String getRecipientPhone() { return recipientPhone; }
    public void setRecipientPhone(String value) { this.recipientPhone = value; }
    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String value) { this.shippingAddress = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { this.status = value; }
    public String getExpressCompany() { return expressCompany; }
    public void setExpressCompany(String value) { this.expressCompany = value; }
    public String getTrackingNo() { return trackingNo; }
    public void setTrackingNo(String value) { this.trackingNo = value; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime value) { this.paidAt = value; }
    public LocalDateTime getShippedAt() { return shippedAt; }
    public void setShippedAt(LocalDateTime value) { this.shippedAt = value; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime value) { this.completedAt = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime value) { this.createdAt = value; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime value) { this.updatedAt = value; }
}

