package com.qianxun.homestay.modules.mall.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 商城退款记录的持久化映射。 */
public class MallRefundEntity {
    /** 退款记录编号。 */
    private Long id;
    /** 商城订单编号。 */
    private Long mallOrderId;
    /** 商城支付编号。 */
    private Long mallPaymentId;
    /** 商户退款单号。 */
    private String refundNo;
    /** 微信退款单号。 */
    private String wechatRefundId;
    /** 退款金额。 */
    private BigDecimal amount;
    /** 退款状态。 */
    private String status;
    /** 退款完成时间。 */
    private LocalDateTime refundedAt;
    public Long getId() { return id; }
    public void setId(Long value) { this.id = value; }
    public Long getMallOrderId() { return mallOrderId; }
    public void setMallOrderId(Long value) { this.mallOrderId = value; }
    public Long getMallPaymentId() { return mallPaymentId; }
    public void setMallPaymentId(Long value) { this.mallPaymentId = value; }
    public String getRefundNo() { return refundNo; }
    public void setRefundNo(String value) { this.refundNo = value; }
    public String getWechatRefundId() { return wechatRefundId; }
    public void setWechatRefundId(String value) { this.wechatRefundId = value; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal value) { this.amount = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { this.status = value; }
    public LocalDateTime getRefundedAt() { return refundedAt; }
    public void setRefundedAt(LocalDateTime value) { this.refundedAt = value; }
}

