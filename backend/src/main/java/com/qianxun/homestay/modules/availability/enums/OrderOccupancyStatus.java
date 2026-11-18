package com.qianxun.homestay.modules.availability.enums;

/** 正式订单在房态判断中使用的稳定状态编码。 */
public enum OrderOccupancyStatus {
    /** 已取消订单，不再占用对应住宿夜晚。 */
    CANCELLED("CANCELLED");

    private final String code;

    OrderOccupancyStatus(String code) {
        this.code = code;
    }

    /** 返回持久化状态编码。 */
    public String getCode() {
        return code;
    }
}
