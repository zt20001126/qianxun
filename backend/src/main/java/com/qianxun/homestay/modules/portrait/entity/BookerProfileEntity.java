package com.qianxun.homestay.modules.portrait.entity;

/** booker_profile 单表的手机号画像持久化映射。 */
public class BookerProfileEntity {

    /** 画像数据库主键。 */
    private Long id;
    /** 无手机号时用于关联单笔订单；有手机号时为空。 */
    private Long orderId;
    /** 手机号生成的身份散列键；手机号缺失时按订单生成。 */
    private String identityKey;
    /** 预订人姓名；从关联订单同步。 */
    private String guestName;
    /** 预订人手机号；从关联订单同步。 */
    private String phone;
    /** 管理员录入或本地证件推算的年龄。 */
    private Integer age;
    /** 固定出行群体编码。 */
    private String partyType;
    /** 管理员填写或由证件号在本地推算的地区。 */
    private String inferredRegion;
    /** 画像标签 JSON 数组文本。 */
    private String tagsJson;
    /** 管理员长期维护的手机号画像备注。 */
    private String adminRemark;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getIdentityKey() { return identityKey; }
    public void setIdentityKey(String identityKey) { this.identityKey = identityKey; }
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
}
