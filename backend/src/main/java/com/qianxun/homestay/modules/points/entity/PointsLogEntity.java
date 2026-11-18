package com.qianxun.homestay.modules.points.entity;

import com.qianxun.homestay.modules.points.enums.PointsLogType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** points_log 表的订单奖励流水映射。 */
public class PointsLogEntity {

    /** 积分流水数据库主键。 */
    private Long id;
    /** 流水归属会员编号。 */
    private Long memberId;
    /** 关联订单编号；与订单无关的流水为空。 */
    private Long orderId;
    /** 积分流水业务类型。 */
    private PointsLogType type;
    /** 本次变更的可用积分数量，可正可负。 */
    private Integer points;
    /** 本次变更的成长积分数量，可正可负。 */
    private Integer growthPoints;
    /** 本次积分变更完成后的可用积分余额。 */
    private Integer balanceAfter;
    /** 本次积分变更关联的金额，金额单位为元。 */
    private BigDecimal amount;
    /** 积分抵扣或返还关联的人民币金额快照。 */
    private BigDecimal discountAmount;
    /** 业务幂等关联编号，用于识别重复流水。 */
    private String refNo;
    /** 积分变更说明。 */
    private String remark;
    /** 流水创建时间。 */
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public PointsLogType getType() { return type; }
    public void setType(PointsLogType type) { this.type = type; }
    public Integer getPoints() { return points; }
    public void setPoints(Integer points) { this.points = points; }
    public Integer getGrowthPoints() { return growthPoints; }
    public void setGrowthPoints(Integer growthPoints) { this.growthPoints = growthPoints; }
    public Integer getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(Integer balanceAfter) { this.balanceAfter = balanceAfter; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public String getRefNo() { return refNo; }
    public void setRefNo(String refNo) { this.refNo = refNo; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
