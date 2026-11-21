package com.qianxun.homestay.modules.order.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.qianxun.homestay.modules.portrait.dto.BookerPortraitSaveRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/** 后台编辑订单资料；未提供的字段保持原值，金额字段不属于可编辑内容。 */
public class UpdateOrderRequest {

    /** 预订联系人姓名。 */
    @Size(max = 80, message = "预订人姓名不能超过 80 个字符")
    private String guestName;
    /** 预订联系人手机号。 */
    @Size(max = 20, message = "联系电话不能超过 20 个字符")
    private String phone;
    /** 入住人数。 */
    @Positive(message = "入住人数必须大于零")
    private Integer guestCount;
    /** 订单备注；如填写取消原因则追加在此字段。 */
    @Size(max = 500, message = "备注不能超过 500 个字符")
    private String remark;
    /** 管理员录单备注；与预订人下单备注分开保存。 */
    @Size(max = 500, message = "管理员备注不能超过 500 个字符")
    private String adminRemark;
    /** 可选更新的顾客画像，和订单资料在同一事务中保存。 */
    @Valid
    private BookerPortraitSaveRequest bookerPortrait;
    /** 调整后的客房编号，仅允许符合状态和库存规则的订单修改。 */
    @Positive(message = "房间编号不正确")
    private Long roomId;
    /** 调整后的入住日期。 */
    private LocalDate checkIn;
    /** 调整后的退房日期。 */
    private LocalDate checkOut;
    /** 保存反序列化时发现的契约外字段，供业务层拒绝未定义输入。 */
    private final Map<String, Object> unknownFields = new LinkedHashMap<>();

    /** 收集契约外字段，以便服务层明确拒绝金额字段和其他未定义输入。 */
    @JsonAnySetter
    public void captureUnknownField(String name, Object value) {
        unknownFields.put(name, value);
    }

    public String getGuestName() { return guestName; }
    public void setGuestName(String guestName) { this.guestName = guestName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Integer getGuestCount() { return guestCount; }
    public void setGuestCount(Integer guestCount) { this.guestCount = guestCount; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getAdminRemark() { return adminRemark; }
    public void setAdminRemark(String adminRemark) { this.adminRemark = adminRemark; }
    public BookerPortraitSaveRequest getBookerPortrait() { return bookerPortrait; }
    public void setBookerPortrait(BookerPortraitSaveRequest bookerPortrait) { this.bookerPortrait = bookerPortrait; }
    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public LocalDate getCheckIn() { return checkIn; }
    public void setCheckIn(LocalDate checkIn) { this.checkIn = checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
    public void setCheckOut(LocalDate checkOut) { this.checkOut = checkOut; }
    public Map<String, Object> getUnknownFields() { return unknownFields; }
}
