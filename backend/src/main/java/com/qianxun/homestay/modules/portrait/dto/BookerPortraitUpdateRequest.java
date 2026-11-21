package com.qianxun.homestay.modules.portrait.dto;

import com.qianxun.homestay.modules.portrait.enums.BookerPartyType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 管理端持续更新手机号画像及其订单备注的请求。 */
public record BookerPortraitUpdateRequest(
        /** 画像年龄；可空。 */
        @Min(value = 0, message = "年龄不能小于 0")
        @Max(value = 120, message = "年龄不能大于 120")
        Integer age,
        /** 主要出行群体；可空。 */
        BookerPartyType partyType,
        /** 可手工编辑或从身份证号在浏览器本地推算的地区；可空。 */
        @Size(max = 120, message = "推断地区不能超过 120 个字符")
        String inferredRegion,
        /** 画像行内保存的已选标签名称列表。 */
        @Size(max = 30, message = "画像标签不能超过 30 个")
        List<@NotBlank @Size(max = 40, message = "单个画像标签不能超过 40 个字符") String> tags,
        /** 可持续编辑的手机号级管理员备注。 */
        @Size(max = 2000, message = "画像管理员备注不能超过 2000 个字符")
        String adminRemark,
        /** 可持续编辑的订单备注；每项订单必须属于当前手机号画像。 */
        @Valid
        @Size(max = 500, message = "单次最多更新 500 笔订单备注")
        List<BookerPortraitOrderRemarkUpdateRequest> orderRemarks) {
}
