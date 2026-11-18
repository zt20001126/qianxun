package com.qianxun.homestay.modules.channel.entity;

import java.time.LocalDateTime;

/** 渠道表映射实体。 */
public class ChannelEntity {

    /** 渠道数据库主键。 */
    private Long id;
    /** 渠道展示名称。 */
    private String name;
    /** 渠道是否启用。 */
    private Boolean enabled;
    /** 渠道记录创建时间。 */
    private LocalDateTime createdAt;
    /** 渠道记录最近更新时间。 */
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
