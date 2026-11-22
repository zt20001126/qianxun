package com.qianxun.homestay.modules.mall.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 商城支付流水与待支付快照。 */
public class MallPaymentEntity {
    /** 支付编号。 */
    private Long id;
    /** 支付单号。 */
    private String paymentNo;
    /** 商城订单。 */
    private Long mallOrderId;
    /** 会员编号。 */
    private Long memberId;
    /** 商品编号。 */
    private Long productId;
    /** 商品名称快照。 */
    private String productName;
    /** 购买数量。 */
    private Integer quantity;
    /** 商品总额。 */
    private BigDecimal totalAmount;
    /** 预占积分。 */
    private Integer pointsUsed;
    /** 应付金额。 */
    private BigDecimal amount;
    /** 收货人。 */
    private String recipientName;
    /** 收货手机。 */
    private String recipientPhone;
    /** 收货地址。 */
    private String shippingAddress;
    /** 支付方式。 */
    private String paymentMethod;
    /** 渠道交易号。 */
    private String transactionId;
    /** 微信预支付标识。 */
    private String wechatPrepayId;
    /** 支付状态。 */
    private String status;
    /** 截止时间。 */
    private LocalDateTime expiresAt;
    /** 付款时间。 */
    private LocalDateTime paidAt;
    /** 创建时间。 */
    private LocalDateTime createdAt;
    /** 更新时间。 */
    private LocalDateTime updatedAt;
    public Long getId() { return id; }
    public void setId(Long value) { this.id = value; }
    public String getPaymentNo() { return paymentNo; }
    public void setPaymentNo(String value) { this.paymentNo = value; }
    public Long getMallOrderId() { return mallOrderId; }
    public void setMallOrderId(Long value) { this.mallOrderId = value; }
    public Long getMemberId() { return memberId; }
    public void setMemberId(Long value) { this.memberId = value; }
    public Long getProductId() { return productId; }
    public void setProductId(Long value) { this.productId = value; }
    public String getProductName() { return productName; }
    public void setProductName(String value) { this.productName = value; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer value) { this.quantity = value; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal value) { this.totalAmount = value; }
    public Integer getPointsUsed() { return pointsUsed; }
    public void setPointsUsed(Integer value) { this.pointsUsed = value; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal value) { this.amount = value; }
    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String value) { this.recipientName = value; }
    public String getRecipientPhone() { return recipientPhone; }
    public void setRecipientPhone(String value) { this.recipientPhone = value; }
    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String value) { this.shippingAddress = value; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String value) { this.paymentMethod = value; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String value) { this.transactionId = value; }
    public String getWechatPrepayId() { return wechatPrepayId; }
    public void setWechatPrepayId(String value) { this.wechatPrepayId = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { this.status = value; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime value) { this.expiresAt = value; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime value) { this.paidAt = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime value) { this.createdAt = value; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime value) { this.updatedAt = value; }
}

