package com.qianxun.homestay.modules.availability.service.impl;

import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.modules.availability.domain.StayDateRange;
import com.qianxun.homestay.modules.availability.enums.BookingLockOccupancyStatus;
import com.qianxun.homestay.modules.availability.enums.OrderOccupancyStatus;
import com.qianxun.homestay.modules.availability.entity.AvailabilityRangeEntity;
import com.qianxun.homestay.modules.availability.mapper.AvailabilityMapper;
import com.qianxun.homestay.modules.availability.service.AvailabilityService;
import com.qianxun.homestay.modules.availability.vo.AppAvailabilityCheckVO;
import com.qianxun.homestay.modules.availability.vo.AppRoomAvailabilityVO;
import com.qianxun.homestay.modules.room.entity.RoomEntity;
import com.qianxun.homestay.modules.room.mapper.RoomMapper;
import com.qianxun.homestay.modules.room.service.RoomPriceService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** 汇总订单、封房和未过期锁房，统一计算住宿区间的可用性。 */
@Service
public class AvailabilityServiceImpl implements AvailabilityService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    /** 小程序预订日期窗口，包含北京时间当天。 */
    private static final int BOOKING_WINDOW_DAYS = 90;

    private final AvailabilityMapper availabilityMapper;
    private final RoomMapper roomMapper;
    private final RoomPriceService roomPriceService;

    public AvailabilityServiceImpl(
            AvailabilityMapper availabilityMapper, RoomMapper roomMapper, RoomPriceService roomPriceService) {
        this.availabilityMapper = availabilityMapper;
        this.roomMapper = roomMapper;
        this.roomPriceService = roomPriceService;
    }

    @Override
    public void validateStayRange(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null || !checkIn.isBefore(checkOut)) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "退房日期必须晚于入住日期");
        }
    }

    @Override
    public void validateMiniProgramRange(LocalDate checkIn, LocalDate checkOut) {
        validateStayRange(checkIn, checkOut);
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        LocalDate lastSelectableDate = today.plusDays(BOOKING_WINDOW_DAYS - 1L);
        if (checkIn.isBefore(today) || checkOut.isBefore(today)
                || checkIn.isAfter(lastSelectableDate) || checkOut.isAfter(lastSelectableDate)) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "入住和退房日期须在北京时间从今天起的 %d 天内".formatted(BOOKING_WINDOW_DAYS));
        }
    }

    @Override
    public boolean isAvailable(Long roomId, LocalDate checkIn, LocalDate checkOut) {
        validateStayRange(checkIn, checkOut);
        return !hasConflict(roomId, checkIn, checkOut, null);
    }

    @Override
    public List<LocalDate> findUnavailableDates(Long roomId, LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || !startDate.isBefore(endDate)) {
            return List.of();
        }
        TreeSet<LocalDate> unavailable = new TreeSet<>();
        for (AvailabilityRangeEntity occupied : overlappingRanges(roomId, startDate, endDate, null, null, null)) {
            LocalDate day = occupied.getStartDate().isAfter(startDate) ? occupied.getStartDate() : startDate;
            LocalDate end = occupied.getEndDate().isBefore(endDate) ? occupied.getEndDate() : endDate;
            while (day.isBefore(end)) {
                unavailable.add(day);
                day = day.plusDays(1);
            }
        }
        return List.copyOf(unavailable);
    }

    @Override
    public AppRoomAvailabilityVO getAppRoomAvailability(Long roomId, YearMonth month) {
        RoomEntity room = roomMapper.findEnabledById(roomId);
        if (room == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "房间不存在");
        }
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        LocalDate lastCheckout = today.plusDays(BOOKING_WINDOW_DAYS - 1L);
        LocalDate windowEnd = lastCheckout.plusDays(1);
        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.plusMonths(1).atDay(1);
        LocalDate start = monthStart.isAfter(today) ? monthStart : today;
        LocalDate end = monthEnd.isBefore(windowEnd) ? monthEnd : windowEnd;
        List<LocalDate> unavailableDates = start.isBefore(end)
                ? findUnavailableDates(roomId, start, end)
                : List.of();
        // 窗口最后一天可以退房，已无法再作为住宿夜晚报价。
        LocalDate priceEnd = end.isBefore(lastCheckout) ? end : lastCheckout;
        Map<LocalDate, String> nightlyPrices = new TreeMap<>();
        if (start.isBefore(priceEnd)) {
            Set<LocalDate> unavailable = new HashSet<>(unavailableDates);
            roomPriceService.quoteNights(room, new StayDateRange(start, priceEnd)).forEach((date, amount) -> {
                if (!unavailable.contains(date)) {
                    nightlyPrices.put(date, amount.toPlainString());
                }
            });
        }
        return new AppRoomAvailabilityVO(roomId, month.toString(), unavailableDates, nightlyPrices);
    }

    @Override
    public AppAvailabilityCheckVO checkAppAvailability(Long roomId, LocalDate checkIn, LocalDate checkOut) {
        validateMiniProgramRange(checkIn, checkOut);
        RoomEntity room = roomMapper.findEnabledById(roomId);
        if (room == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "房间不存在");
        }
        StayDateRange stay = new StayDateRange(checkIn, checkOut);
        int nights = Math.toIntExact(stay.nights());
        BigDecimal roomAmount = roomPriceService.quote(room, stay);
        return new AppAvailabilityCheckVO(
                !hasConflict(roomId, checkIn, checkOut, null), nights, roomAmount.toPlainString());
    }

    @Override
    public void lockRoomsForUpdate(Collection<Long> roomIds) {
        roomIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .forEach(roomId -> {
                    if (availabilityMapper.lockRoom(roomId) == null) {
                        throw new BusinessException(HttpStatus.NOT_FOUND, "房间不存在");
                    }
                });
    }

    @Override
    public boolean hasConflict(Long roomId, LocalDate startDate, LocalDate endDate, Long excludedBlockId) {
        return !overlappingRanges(roomId, startDate, endDate, excludedBlockId, null, null).isEmpty();
    }

    @Override
    public boolean hasConflictExcludingOrder(
            Long roomId, LocalDate startDate, LocalDate endDate, Long excludedOrderId) {
        return !overlappingRanges(roomId, startDate, endDate, null, excludedOrderId, null).isEmpty();
    }

    @Override
    public boolean hasConflictExcludingBookingLock(
            Long roomId, LocalDate startDate, LocalDate endDate, Long excludedBookingLockId) {
        return !overlappingRanges(roomId, startDate, endDate, null, null, excludedBookingLockId).isEmpty();
    }

    private List<AvailabilityRangeEntity> overlappingRanges(
            Long roomId, LocalDate startDate, LocalDate endDate,
            Long excludedBlockId, Long excludedOrderId, Long excludedBookingLockId) {
        return availabilityMapper.findOverlappingRanges(
                roomId,
                startDate,
                endDate,
                LocalDateTime.now(BUSINESS_ZONE),
                OrderOccupancyStatus.CANCELLED.getCode(),
                BookingLockOccupancyStatus.PENDING.getCode(),
                excludedBlockId,
                excludedOrderId,
                excludedBookingLockId);
    }
}
