package com.qianxun.homestay.modules.mall.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 商城商品持久化对象。 */
public class MallProductEntity {
    /** 商品主键。 */ private Long id;
    /** 商品名称。 */ private String name;
    /** 商品介绍及规格。 */ private String description;
    /** 商品图片地址。 */ private List<String> images;
    /** 当前单价，单位元。 */ private BigDecimal price;
    /** 上架状态。 */ private String status;
    /** 创建时间，按北京时间存储。 */ private LocalDateTime createdAt;
    /** 更新时间，按北京时间存储。 */ private LocalDateTime updatedAt;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
