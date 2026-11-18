package com.qianxun.homestay.modules.payment.entity;

import com.qianxun.homestay.modules.payment.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** payments 表的模拟支付流水映射。 */
public class PaymentEntity {

    /** 支付流水数据库主键。 */
    private Long id;
    /** 支付流水对应的临时预订编号。 */
    private Long bookingId;
    /** 支付成功后关联的正式订单编号。 */
    private Long orderId;
    /** 服务端生成的商户支付单号。 */
    private String paymentNo;
    /** 预留的微信交易号；第一期模拟支付始终为空。 */
    private String transactionId;
    /** 后端计算的积分抵扣后应付金额。 */
    private BigDecimal amount;
    /** 当前支付流水状态。 */
    private PaymentStatus status;
    /** 模拟支付成功时间，按北京时间存储。 */
    private LocalDateTime paidAt;
    /** 支付流水创建时间。 */
    private LocalDateTime createdAt;
    /** 支付流水最近更新时间。 */
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getPaymentNo() { return paymentNo; }
    public void setPaymentNo(String paymentNo) { this.paymentNo = paymentNo; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
