package com.qianxun.homestay.modules.points.dto;

import com.qianxun.homestay.modules.points.enums.PointsLogType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** 管理员积分发放请求，request_no 用于确保重复提交不会重复入账。 */
public record PointsGrantRequest(
        /** HISTORY_REWARD 同时增加成长积分，GIFT 只增加可用积分。 */
        @NotNull(message = "请选择积分发放类型") PointsLogType type,
        /** 本次发放的正整数积分。 */
        @NotNull(message = "发放积分不能为空") @Positive(message = "发放积分必须为正整数") Integer points,
        /** 可选的关联订单 ID。 */
        @Positive(message = "关联订单编号不正确") Long orderId,
        /** 可选发放原因。 */
        @Size(max = 300, message = "备注不能超过 300 个字符") String remark,
        /** 稳定请求编号，用于重复提交幂等识别。 */
        @NotBlank(message = "请求编号不能为空") @Size(max = 80, message = "请求编号不能超过 80 个字符") String requestNo) {
}
