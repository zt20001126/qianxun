package com.qianxun.homestay.modules.creator.entity;

import java.time.LocalDateTime;

/** 达人表映射实体。 */
public class CreatorEntity {

    /** 达人数据库主键。 */
    private Long id;
    /** 达人名称。 */
    private String name;
    /** 达人所属渠道编号。 */
    private Long channelId;
    /** 达人是否启用。 */
    private Boolean enabled;
    /** 达人记录创建时间。 */
    private LocalDateTime createdAt;
    /** 达人记录最近更新时间。 */
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getChannelId() { return channelId; }
    public void setChannelId(Long channelId) { this.channelId = channelId; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
