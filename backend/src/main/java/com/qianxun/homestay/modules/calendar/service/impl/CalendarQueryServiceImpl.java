package com.qianxun.homestay.modules.calendar.service.impl;

import com.qianxun.homestay.modules.calendar.enums.CalendarCellStatus;
import com.qianxun.homestay.modules.calendar.mapper.CalendarMapper;
import com.qianxun.homestay.modules.calendar.service.CalendarQueryService;
import com.qianxun.homestay.modules.calendar.vo.CalendarDayVO;
import com.qianxun.homestay.modules.calendar.vo.CalendarRoomVO;
import com.qianxun.homestay.modules.calendar.vo.CalendarVO;
import com.qianxun.homestay.modules.order.entity.OrderEntity;
import com.qianxun.homestay.modules.order.enums.OrderStatus;
import com.qianxun.homestay.modules.room.entity.RoomEntity;
import com.qianxun.homestay.modules.roomblock.entity.RoomBlockEntity;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 汇总订单与封房区间，生成不落库的月度房态格子。 */
@Service
public class CalendarQueryServiceImpl implements CalendarQueryService {

    private final CalendarMapper calendarMapper;

    public CalendarQueryServiceImpl(CalendarMapper calendarMapper) {
        this.calendarMapper = calendarMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public CalendarVO findMonth(YearMonth month) {
        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.plusMonths(1).atDay(1);
        List<RoomEntity> rooms = calendarMapper.findRooms();
        Map<Long, Map<LocalDate, CalendarDayVO>> roomDays = initializeRoomDays(rooms, monthStart, monthEnd);

        // 后台月历按契约隐藏待支付锁房；订单和封房的写操作仍由统一房态服务复核锁房。
        List<RoomBlockEntity> blocks = calendarMapper.findOverlappingBlocks(monthStart, monthEnd);
        for (RoomBlockEntity block : blocks) {
            Map<LocalDate, CalendarDayVO> days = roomDays.get(block.getRoomId());
            if (days != null) {
                markBlockedDays(days, block, monthStart, monthEnd);
            }
        }

        List<OrderEntity> orders = calendarMapper.findOverlappingOrders(
                monthStart, monthEnd, OrderStatus.CANCELLED.name());
        for (OrderEntity order : orders) {
            Map<LocalDate, CalendarDayVO> days = roomDays.get(order.getRoomId());
            if (days != null && order.getStatus() != OrderStatus.CANCELLED) {
                markOrderDays(days, order, monthStart, monthEnd);
            }
        }

        List<CalendarRoomVO> calendarRooms = rooms.stream()
                .map(room -> new CalendarRoomVO(room.getId(), room.getNumber(),
                        new ArrayList<>(roomDays.get(room.getId()).values())))
                .toList();
        return new CalendarVO(month.toString(), calendarRooms);
    }

    private Map<Long, Map<LocalDate, CalendarDayVO>> initializeRoomDays(
            List<RoomEntity> rooms, LocalDate monthStart, LocalDate monthEnd) {
        Map<Long, Map<LocalDate, CalendarDayVO>> roomDays = new LinkedHashMap<>();
        for (RoomEntity room : rooms) {
            Map<LocalDate, CalendarDayVO> days = new LinkedHashMap<>();
            for (LocalDate date = monthStart; date.isBefore(monthEnd); date = date.plusDays(1)) {
                days.put(date, new CalendarDayVO(date, CalendarCellStatus.AVAILABLE, null, null));
            }
            roomDays.put(room.getId(), days);
        }
        return roomDays;
    }

    private void markBlockedDays(
            Map<LocalDate, CalendarDayVO> days, RoomBlockEntity block,
            LocalDate monthStart, LocalDate monthEnd) {
        LocalDate start = laterDate(block.getStartDate(), monthStart);
        LocalDate end = earlierDate(block.getEndDate(), monthEnd);
        for (LocalDate date = start; date.isBefore(end); date = date.plusDays(1)) {
            days.put(date, new CalendarDayVO(date, CalendarCellStatus.BLOCKED, null, block.getId()));
        }
    }

    private void markOrderDays(
            Map<LocalDate, CalendarDayVO> days, OrderEntity order,
            LocalDate monthStart, LocalDate monthEnd) {
        LocalDate start = laterDate(order.getCheckIn(), monthStart);
        LocalDate end = earlierDate(order.getCheckOut(), monthEnd);
        for (LocalDate date = start; date.isBefore(end); date = date.plusDays(1)) {
            CalendarCellStatus status = date.equals(order.getCheckIn())
                    ? CalendarCellStatus.fromOrderStatus(order.getStatus())
                    : CalendarCellStatus.OCCUPIED;
            days.put(date, new CalendarDayVO(date, status, order.getId(), null));
        }
    }

    private LocalDate laterDate(LocalDate left, LocalDate right) {
        return left.isAfter(right) ? left : right;
    }

    private LocalDate earlierDate(LocalDate left, LocalDate right) {
        return left.isBefore(right) ? left : right;
    }
}
