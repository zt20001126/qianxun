package com.qianxun.homestay.modules.refund.entity;

import com.qianxun.homestay.modules.refund.enums.RefundStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** refunds 表的持久化映射。 */
public class RefundEntity {

    /** 退款流水数据库主键。 */
    private Long id;
    /** 被取消订单的数据库主键。 */
    private Long orderId;
    /** 原支付流水的数据库主键。 */
    private Long paymentId;
    /** 服务端生成且全局唯一的商户退款单号。 */
    private String refundNo;
    /** 微信退款单号；模拟退款为空。 */
    private String wechatRefundId;
    /** 退款金额与模拟支付实付金额保持一致。 */
    private BigDecimal amount;
    /** 当前退款处理状态。 */
    private RefundStatus status;
    /** 顾客填写的取消原因；失败说明也使用同一契约字段。 */
    private String reason;
    /** 支付渠道确认退款完成的时间，按北京时间存储。 */
    private LocalDateTime refundedAt;
    /** 退款流水创建时间，按北京时间存储。 */
    private LocalDateTime createdAt;
    /** 退款流水最近更新时间，按北京时间存储。 */
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }
    public String getRefundNo() { return refundNo; }
    public void setRefundNo(String refundNo) { this.refundNo = refundNo; }
    public String getWechatRefundId() { return wechatRefundId; }
    public void setWechatRefundId(String wechatRefundId) { this.wechatRefundId = wechatRefundId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public RefundStatus getStatus() { return status; }
    public void setStatus(RefundStatus status) { this.status = status; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public LocalDateTime getRefundedAt() { return refundedAt; }
    public void setRefundedAt(LocalDateTime refundedAt) { this.refundedAt = refundedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
