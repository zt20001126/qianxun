package com.qianxun.homestay.modules.portrait.dto;

import com.qianxun.homestay.modules.portrait.enums.BookerPartyType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 管理端保存预订人画像的请求；不接收身份证号。 */
public record BookerPortraitSaveRequest(
        /** 预订人年龄；可空。 */
        @Min(value = 0, message = "年龄不能小于 0")
        @Max(value = 120, message = "年龄不能大于 120")
        Integer age,
        /** 主要出行群体；可空。 */
        BookerPartyType partyType,
        /** 可手工编辑或从身份证号在浏览器本地推算的地区；可空。 */
        @Size(max = 120, message = "推断地区不能超过 120 个字符")
        String inferredRegion,
        /** 从预置/管理员新增选项中选择的标签名称；可空或空列表。 */
        @Size(max = 30, message = "画像标签不能超过 30 个")
        List<@NotBlank @Size(max = 40, message = "单个画像标签不能超过 40 个字符") String> tags) {
}
