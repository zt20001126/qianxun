package com.qianxun.homestay.modules.portrait.vo;

import java.util.List;

/** 订单详情和办理入住表单使用的预订人画像数据。 */
public record BookerPortraitVO(
        /** 顾客画像主键，供后台画像独立编辑使用。 */
        Long id,
        /** 画像年龄；资料缺失时为空。 */
        Integer age,
        /** 主要出行群体编码；资料缺失时为空。 */
        String partyType,
        /** 身份证号在浏览器本地推算的省级地区。 */
        String inferredRegion,
        /** 当前手机号画像的可编辑标签名称列表。 */
        List<String> tags) {
}
