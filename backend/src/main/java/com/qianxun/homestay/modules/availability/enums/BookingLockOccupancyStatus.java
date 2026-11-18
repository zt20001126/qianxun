package com.qianxun.homestay.modules.availability.enums;

/** 临时锁房在房态判断中占用库存的稳定状态编码。 */
public enum BookingLockOccupancyStatus {
    /** 待支付且尚未过期的锁房记录，占用对应住宿夜晚。 */
    PENDING("PENDING");

    private final String code;

    BookingLockOccupancyStatus(String code) {
        this.code = code;
    }

    /** 返回持久化状态编码。 */
    public String getCode() {
        return code;
    }
}
