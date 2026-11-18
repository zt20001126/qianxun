package com.qianxun.homestay.modules.member.entity;

import java.time.LocalDateTime;

/**
 * 会员持久化实体，仅映射数据库 members 表。
 */
public class MemberEntity {

    /** 会员数据库主键，也是会员 JWT 身份标识。 */
    private Long id;
    /** 会员登录手机号，数据库唯一去重。 */
    private String phone;
    /** 预留微信 OpenID；本阶段始终为空。 */
    private String openid;
    /** 固定由手机号后四位生成的昵称。 */
    private String nickname;
    /** 系统统一默认头像地址。 */
    private String avatar;
    /** 累计成长积分。 */
    private Integer growthPoints;
    /** 当前可用积分。 */
    private Integer points;
    /** 会员记录创建时间。 */
    private LocalDateTime createdAt;
    /** 会员记录更新时间。 */
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getOpenid() { return openid; }
    public void setOpenid(String openid) { this.openid = openid; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public Integer getGrowthPoints() { return growthPoints; }
    public void setGrowthPoints(Integer growthPoints) { this.growthPoints = growthPoints; }
    public Integer getPoints() { return points; }
    public void setPoints(Integer points) { this.points = points; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
