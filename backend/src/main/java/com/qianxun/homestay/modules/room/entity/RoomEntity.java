package com.qianxun.homestay.modules.room.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 客房表映射实体。 */
public class RoomEntity {

    /** 客房数据库主键。 */
    private Long id;
    /** 客房对外房号。 */
    private String number;
    /** 客房名称。 */
    private String name;
    /** 客房所在楼层。 */
    private Integer floor;
    /** 客房每晚基础价格，单位为元。 */
    private BigDecimal price;
    /** 客房允许入住的最大人数。 */
    private Integer capacity;
    /** 客房床型说明。 */
    private String bedInfo;
    /** 客房建筑面积，单位为平方米。 */
    private BigDecimal area;
    /** 客房设施名称列表，对应数据库 JSON 字段。 */
    private List<String> facilities;
    /** 客房图片地址列表，对应数据库 JSON 字段。 */
    private List<String> images;
    /** 客房介绍。 */
    private String description;
    /** 客房是否上架可供预订。 */
    private Boolean enabled;
    /** 客房记录创建时间。 */
    private LocalDateTime createdAt;
    /** 客房记录最近更新时间。 */
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getFloor() { return floor; }
    public void setFloor(Integer floor) { this.floor = floor; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
    public String getBedInfo() { return bedInfo; }
    public void setBedInfo(String bedInfo) { this.bedInfo = bedInfo; }
    public BigDecimal getArea() { return area; }
    public void setArea(BigDecimal area) { this.area = area; }
    public List<String> getFacilities() { return facilities; }
    public void setFacilities(List<String> facilities) { this.facilities = facilities; }
    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
