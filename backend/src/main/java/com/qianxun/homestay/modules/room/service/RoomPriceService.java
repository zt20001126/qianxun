package com.qianxun.homestay.modules.room.service;

import com.qianxun.homestay.modules.availability.domain.StayDateRange;
import com.qianxun.homestay.modules.room.entity.RoomEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** 客房住宿区间报价服务。 */
public interface RoomPriceService {

    /** 计算指定客房在已校验住宿区间内的房费。 */
    BigDecimal quote(RoomEntity room, StayDateRange stay);

    /** 用同一份住宿区间日期配置计算各房间的房费，按房间编号索引。 */
    Map<Long, BigDecimal> quoteAll(List<RoomEntity> rooms, StayDateRange stay);

    /** 一次读取区间内的节假日配置，返回每个住宿夜晚的房费。 */
    Map<LocalDate, BigDecimal> quoteNights(RoomEntity room, StayDateRange stay);
}
