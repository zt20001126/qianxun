package com.qianxun.homestay.modules.availability.service;

import com.qianxun.homestay.modules.availability.vo.AppAvailabilityCheckVO;
import com.qianxun.homestay.modules.availability.vo.AppRoomAvailabilityVO;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collection;
import java.util.List;

/** 提供小程序和后台共用的日期冲突判断。 */
public interface AvailabilityService {

    /** 校验入住、退房日期构成有效住宿区间。 */
    void validateStayRange(LocalDate checkIn, LocalDate checkOut);

    /** 校验小程序日期落在北京时间起算的连续 60 个自然日内。 */
    void validateMiniProgramRange(LocalDate checkIn, LocalDate checkOut);

    /** 判断指定住宿区间是否未被订单、封房或有效锁房占用。 */
    boolean isAvailable(Long roomId, LocalDate checkIn, LocalDate checkOut);

    /** 返回指定窗口内不可订的住宿夜晚。 */
    List<LocalDate> findUnavailableDates(Long roomId, LocalDate startDate, LocalDate endDate);

    /** 查询小程序月历，并截断到未来 60 天窗口。 */
    AppRoomAvailabilityVO getAppRoomAvailability(Long roomId, YearMonth month);

    /** 复核日期区间并计算当前基础房价总额。 */
    AppAvailabilityCheckVO checkAppAvailability(Long roomId, LocalDate checkIn, LocalDate checkOut);

    /** 按房间 ID 升序加锁，供封房等房态写操作共用。 */
    void lockRoomsForUpdate(Collection<Long> roomIds);

    /** 判断区间是否冲突，可排除正在修改的封房记录。 */
    boolean hasConflict(Long roomId, LocalDate startDate, LocalDate endDate, Long excludedBlockId);

    /** 判断指定区间是否被订单、封房或有效锁房占用，并可排除当前订单。 */
    boolean hasConflictExcludingOrder(Long roomId, LocalDate startDate, LocalDate endDate, Long excludedOrderId);

    /** 支付时在房间行锁内复核库存，并排除当前支付对应的临时锁房记录。 */
    boolean hasConflictExcludingBookingLock(
            Long roomId, LocalDate startDate, LocalDate endDate, Long excludedBookingLockId);
}
