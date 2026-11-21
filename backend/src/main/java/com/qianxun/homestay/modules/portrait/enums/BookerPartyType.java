package com.qianxun.homestay.modules.portrait.enums;

/** 预订人画像中的固定出行群体分类编码。 */
public enum BookerPartyType {
    SOLO("独自出行"),
    COUPLE("情侣/夫妻"),
    PARENT_CHILD("亲子家庭"),
    FAMILY_FRIENDS("家庭/亲友"),
    FRIENDS("朋友结伴"),
    BUSINESS("商务/差旅"),
    GROUP("团队/团体"),
    OTHER("其他");

    private final String displayName;

    BookerPartyType(String displayName) {
        this.displayName = displayName;
    }

    /** 返回后台展示的群体名称。 */
    public String getDisplayName() {
        return displayName;
    }
}
