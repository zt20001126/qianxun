package com.qianxun.homestay.modules.order.dto;

import com.qianxun.homestay.modules.order.enums.OrderStatus;
import com.qianxun.homestay.modules.portrait.dto.BookerPortraitSaveRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** 后台手工录入第三方订单的请求参数。 */
public record CreateExternalOrderRequest(
        /** 第三方订单来源渠道编号。 */
        @NotNull(message = "请选择订单渠道") @Positive(message = "渠道编号不正确") Long channelId,
        /** 订单关联客房编号。 */
        @NotNull(message = "请选择房间") @Positive(message = "房间编号不正确") Long roomId,
        /** 订单关联达人编号；无达人时为空。 */
        @Positive(message = "达人编号不正确") Long creatorId,
        /** 第三方平台订单编号。 */
        @Size(max = 100, message = "平台订单号不能超过 100 个字符") String platformOrderNo,
        /** 预订联系人姓名。 */
        @Size(max = 80, message = "预订人姓名不能超过 80 个字符") String guestName,
        /** 预订联系人手机号。 */
        @Size(max = 20, message = "联系电话不能超过 20 个字符") String phone,
        /** 入住人数。 */
        @Positive(message = "入住人数必须大于零") Integer guestCount,
        /** 预定入住日期。 */
        @NotNull(message = "请选择入住日期") LocalDate checkIn,
        /** 预定退房日期，不占用当天房晚。 */
        @NotNull(message = "请选择退房日期") LocalDate checkOut,
        /** 创建订单时设置的初始状态。 */
        @NotNull(message = "请选择订单状态") OrderStatus status,
        /** 订单备注。 */
        @Size(max = 500, message = "备注不能超过 500 个字符") String remark,
        /** 创建后不可编辑的金额快照输入。 */
        @NotNull(message = "请填写订单金额") @Valid OrderAmountRequest amount,
        /** 管理员录入订单时填写的内部备注。 */
        @Size(max = 500, message = "管理员备注不能超过 500 个字符") String adminRemark,
        /** 第三方订单创建时可选填写的预订人画像。 */
        @Valid BookerPortraitSaveRequest bookerPortrait,
        /** 第三方平台原始下单日期，可为空。 */
        LocalDate platformOrderDate) {
}
