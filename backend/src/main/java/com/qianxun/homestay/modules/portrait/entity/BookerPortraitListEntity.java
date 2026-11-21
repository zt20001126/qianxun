package com.qianxun.homestay.modules.portrait.entity;

import java.math.BigDecimal;

/** 顾客画像列表查询及已完成订单聚合结果。 */
public class BookerPortraitListEntity {

    /** 顾客画像主键。 */
    private Long id;
    /** 预订人姓名。 */
    private String guestName;
    /** 完整预订人手机号。 */
    private String phone;
    /** 年龄。 */
    private Integer age;
    /** 固定群体编码。 */
    private String partyType;
    /** 管理员填写或由证件号在本地推算的地区。 */
    private String inferredRegion;
    /** 画像标签 JSON 数组文本。 */
    private String tagsJson;
    /** 管理员长期维护的手机号画像备注。 */
    private String adminRemark;
    /** 该手机号是否关联会员记录。 */
    private Boolean member;
    /** 已完成订单数量。 */
    private Long completedOrderCount;
    /** 已完成订单实付金额合计。 */
    private BigDecimal cumulativePaidAmount;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getGuestName() { return guestName; }
    public void setGuestName(String guestName) { this.guestName = guestName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public String getPartyType() { return partyType; }
    public void setPartyType(String partyType) { this.partyType = partyType; }
    public String getInferredRegion() { return inferredRegion; }
    public void setInferredRegion(String inferredRegion) { this.inferredRegion = inferredRegion; }
    public String getTagsJson() { return tagsJson; }
    public void setTagsJson(String tagsJson) { this.tagsJson = tagsJson; }
    public String getAdminRemark() { return adminRemark; }
    public void setAdminRemark(String adminRemark) { this.adminRemark = adminRemark; }
    public Boolean getMember() { return member; }
    public void setMember(Boolean member) { this.member = member; }
    public Long getCompletedOrderCount() { return completedOrderCount; }
    public void setCompletedOrderCount(Long completedOrderCount) { this.completedOrderCount = completedOrderCount; }
    public BigDecimal getCumulativePaidAmount() { return cumulativePaidAmount; }
    public void setCumulativePaidAmount(BigDecimal cumulativePaidAmount) { this.cumulativePaidAmount = cumulativePaidAmount; }
}
