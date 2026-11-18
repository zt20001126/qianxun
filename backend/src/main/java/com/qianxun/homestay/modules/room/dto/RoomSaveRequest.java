package com.qianxun.homestay.modules.room.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/** R07/R08 客房资料和基础价格请求。 */
public class RoomSaveRequest {

    @NotBlank(message = "房号不能为空")
    @Size(max = 10, message = "房号不能超过 10 个字符")
    private String number;

    @NotBlank(message = "房间名称不能为空")
    @Size(max = 80, message = "房间名称不能超过 80 个字符")
    private String name;

    @NotNull(message = "楼层不能为空")
    @Min(value = 1, message = "楼层必须大于 0")
    @Max(value = Short.MAX_VALUE, message = "楼层超出允许范围")
    private Integer floor;

    @NotNull(message = "每晚房价不能为空")
    @DecimalMin(value = "0.00", message = "每晚房价不能为负数")
    @Digits(integer = 10, fraction = 2, message = "每晚房价最多两位小数")
    private BigDecimal price;

    @NotNull(message = "最大入住人数不能为空")
    @Min(value = 1, message = "最大入住人数必须大于 0")
    @Max(value = 65535, message = "最大入住人数超出允许范围")
    private Integer capacity;

    @Size(max = 120, message = "床型信息不能超过 120 个字符")
    private String bedInfo;

    @Digits(integer = 4, fraction = 2, message = "面积最多两位小数")
    @DecimalMin(value = "0.00", message = "面积不能为负数")
    private BigDecimal area;

    @Size(max = 100, message = "设施数量不能超过 100 项")
    private List<@NotBlank(message = "设施内容不能为空") @Size(max = 100, message = "单项设施不能超过 100 个字符") String> facilities;

    @Size(max = 50, message = "图片数量不能超过 50 张")
    private List<@NotBlank(message = "图片地址不能为空") @Size(max = 2048, message = "图片地址过长") String> images;

    private String description;

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
}
