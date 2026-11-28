package com.qianxun.homestay.modules.room.service.impl;

import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.modules.availability.domain.StayDateRange;
import com.qianxun.homestay.modules.room.entity.RoomEntity;
import com.qianxun.homestay.modules.room.mapper.HolidayPriceDateMapper;
import com.qianxun.homestay.modules.room.service.RoomPriceService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** 使用两档金额和数据库中的启用日期计算住宿原价。 */
@Service
public class RoomPriceServiceImpl implements RoomPriceService {

    /** 锁房及订单金额字段 DECIMAL(12,2) 可存储的最大值。 */
    private static final BigDecimal MAX_ROOM_AMOUNT = new BigDecimal("9999999999.99");

    private final HolidayPriceDateMapper holidayPriceDateMapper;

    public RoomPriceServiceImpl(HolidayPriceDateMapper holidayPriceDateMapper) {
        this.holidayPriceDateMapper = holidayPriceDateMapper;
    }

    @Override
    public BigDecimal quote(RoomEntity room, StayDateRange stay) {
        return calculate(room, stay.nights(), countHolidayNights(stay));
    }

    @Override
    public Map<Long, BigDecimal> quoteAll(List<RoomEntity> rooms, StayDateRange stay) {
        if (rooms.isEmpty()) {
            return Map.of();
        }
        long nights = stay.nights();
        long holidayNights = countHolidayNights(stay);
        Map<Long, BigDecimal> amounts = new HashMap<>();
        for (RoomEntity room : rooms) {
            amounts.put(room.getId(), calculate(room, nights, holidayNights));
        }
        return amounts;
    }

    @Override
    public Map<LocalDate, BigDecimal> quoteNights(RoomEntity room, StayDateRange stay) {
        Set<LocalDate> holidayDates = new HashSet<>(findHolidayNights(stay));
        BigDecimal regularPrice = calculate(room, 1, 0);
        BigDecimal holidayPrice = calculate(room, 1, 1);
        Map<LocalDate, BigDecimal> amounts = new HashMap<>();
        for (LocalDate date = stay.checkIn(); date.isBefore(stay.checkOut()); date = date.plusDays(1)) {
            amounts.put(date, holidayDates.contains(date) ? holidayPrice : regularPrice);
        }
        return amounts;
    }

    private long countHolidayNights(StayDateRange stay) {
        return findHolidayNights(stay).size();
    }

    private List<LocalDate> findHolidayNights(StayDateRange stay) {
        // 退房当天不住宿，也就不计入任一价格档位。
        return holidayPriceDateMapper.findDates(stay.checkIn(), stay.checkOut().minusDays(1));
    }

    private BigDecimal calculate(RoomEntity room, long nights, long holidayNights) {
        BigDecimal regularPrice = room.getPrice();
        BigDecimal holidayPrice = Objects.requireNonNullElse(room.getHolidayPrice(), regularPrice);
        BigDecimal amount = regularPrice.multiply(BigDecimal.valueOf(nights - holidayNights))
                .add(holidayPrice.multiply(BigDecimal.valueOf(holidayNights)))
                .setScale(2, RoundingMode.UNNECESSARY);
        if (amount.compareTo(MAX_ROOM_AMOUNT) > 0) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "住宿房费超过可预订金额上限");
        }
        return amount;
    }
}
