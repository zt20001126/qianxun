package com.qianxun.homestay.modules.availability.vo;

import java.time.LocalDate;
import java.util.List;

/** R03 月度日历所需的不可订住宿夜晚。 */
public record AppRoomAvailabilityVO(Long roomId, String month, List<LocalDate> unavailableDates) {
}
