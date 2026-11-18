package com.qianxun.homestay.modules.order.entity;

import java.math.BigDecimal;

/** order_amount 表的金额快照映射；net_amount 仅从数据库读取。 */
public class OrderAmountEntity {

    /** 金额快照数据库主键。 */
    private Long id;
    /** 金额快照所属订单编号。 */
    private Long orderId;
    /** 订单实付金额快照。 */
    private BigDecimal paidAmount;
    /** 平台手续费快照。 */
    private BigDecimal platformFee;
    /** 渠道达人提成快照。 */
    private BigDecimal platformCommission;
    /** 由后端按预期收入计算的额外提成快照。 */
    private BigDecimal extraCommission;
    /** 有达人时必填的预期收入快照。 */
    private BigDecimal expectedAmount;
    /** 数据库生成的商家到手金额。 */
    private BigDecimal netAmount;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }
    public BigDecimal getPlatformFee() { return platformFee; }
    public void setPlatformFee(BigDecimal platformFee) { this.platformFee = platformFee; }
    public BigDecimal getPlatformCommission() { return platformCommission; }
    public void setPlatformCommission(BigDecimal platformCommission) { this.platformCommission = platformCommission; }
    public BigDecimal getExtraCommission() { return extraCommission; }
    public void setExtraCommission(BigDecimal extraCommission) { this.extraCommission = extraCommission; }
    public BigDecimal getExpectedAmount() { return expectedAmount; }
    public void setExpectedAmount(BigDecimal expectedAmount) { this.expectedAmount = expectedAmount; }
    public BigDecimal getNetAmount() { return netAmount; }
    public void setNetAmount(BigDecimal netAmount) { this.netAmount = netAmount; }
}
