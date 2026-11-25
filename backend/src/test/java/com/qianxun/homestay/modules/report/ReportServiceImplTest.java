package com.qianxun.homestay.modules.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.qianxun.homestay.modules.channel.mapper.ChannelMapper;
import com.qianxun.homestay.modules.creator.mapper.CreatorMapper;
import com.qianxun.homestay.modules.expense.mapper.ExpenseMapper;
import com.qianxun.homestay.modules.order.enums.OrderStatus;
import com.qianxun.homestay.modules.report.entity.ReportBlockEntity;
import com.qianxun.homestay.modules.report.entity.DashboardRoomEntity;
import com.qianxun.homestay.modules.report.entity.ReportOrderEntity;
import com.qianxun.homestay.modules.report.enums.ReportScope;
import com.qianxun.homestay.modules.report.mapper.ReportMapper;
import com.qianxun.homestay.modules.report.service.impl.ReportServiceImpl;
import com.qianxun.homestay.modules.report.vo.ReportDimensionItemVO;
import com.qianxun.homestay.modules.report.vo.ReportOverviewVO;
import com.qianxun.homestay.modules.report.vo.RoomReportDimensionItemVO;
import com.qianxun.homestay.modules.room.entity.RoomEntity;
import com.qianxun.homestay.modules.room.mapper.RoomMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** 经营报表按住宿夜晚分摊、状态范围及入住率口径的回归测试。 */
class ReportServiceImplTest {

    private static final String CANCELLED_STATUS = OrderStatus.CANCELLED.name();

    private final ReportMapper reportMapper = mock(ReportMapper.class);
    private final ExpenseMapper expenseMapper = mock(ExpenseMapper.class);
    private final RoomMapper roomMapper = mock(RoomMapper.class);
    private final ChannelMapper channelMapper = mock(ChannelMapper.class);
    private final CreatorMapper creatorMapper = mock(CreatorMapper.class);
    private final ReportServiceImpl service = new ReportServiceImpl(
            reportMapper, expenseMapper, roomMapper, channelMapper, creatorMapper);

    @BeforeEach
    void resetMocks() {
        org.mockito.Mockito.reset(reportMapper, expenseMapper, roomMapper, channelMapper, creatorMapper);
    }

    @Test
    void allocatesCrossMonthMoneyAndPutsCentRemainderOnTheLastNight() {
        YearMonth month = YearMonth.of(2026, 2);
        ReportOrderEntity order = order(1L, 1L, LocalDate.of(2026, 1, 31),
                LocalDate.of(2026, 2, 3), OrderStatus.PENDING_CHECKIN,
                "100.01", "10.01", "3.01", "2.01");
        prepare(month, List.of(room(1L, "101")), List.of(order), List.of(), new BigDecimal("0.00"));

        ReportOverviewVO result = service.overview(month, ReportScope.ALL);

        assertThat(result.orderCount()).isZero();
        assertThat(result.nightCount()).isEqualTo(2);
        assertThat(result.paidAmount()).isEqualTo("66.68");
        assertThat(result.platformFee()).isEqualTo("6.68");
        assertThat(result.platformCommission()).isEqualTo("2.01");
        assertThat(result.extraCommission()).isEqualTo("1.34");
        assertThat(result.netAmount()).isEqualTo("56.65");
    }

    @Test
    void filtersOrderMetricsButKeepsOperatingProfitAndOccupancyOnCheckedInOrders() {
        YearMonth month = YearMonth.of(2026, 1);
        ReportOrderEntity pending = order(1L, 1L, LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 3), OrderStatus.PENDING_CONFIRM,
                "100.00", "0.00", "0.00", "0.00");
        ReportOrderEntity checkedIn = order(2L, 1L, LocalDate.of(2026, 1, 10),
                LocalDate.of(2026, 1, 12), OrderStatus.CHECKED_IN,
                "50.00", "0.00", "0.00", "0.00");
        prepare(month, List.of(room(1L, "101")), List.of(pending, checkedIn), List.of(),
                new BigDecimal("10.00"));

        ReportOverviewVO notCheckedIn = service.overview(month, ReportScope.NOT_CHECKED_IN);
        ReportOverviewVO checkedInOnly = service.overview(month, ReportScope.CHECKED_IN);

        assertThat(notCheckedIn.orderCount()).isEqualTo(1);
        assertThat(notCheckedIn.nightCount()).isEqualTo(2);
        assertThat(notCheckedIn.paidAmount()).isEqualTo("100.00");
        assertThat(notCheckedIn.checkedInOrderRevenue()).isEqualTo("0.00");
        assertThat(notCheckedIn.operatingProfit()).isEqualTo("40.00");
        assertThat(notCheckedIn.occupancyRate()).isEqualByComparingTo("0.0645");
        assertThat(checkedInOnly.orderCount()).isEqualTo(1);
        assertThat(checkedInOnly.paidAmount()).isEqualTo("50.00");
        assertThat(checkedInOnly.unoccupiedOrderRevenue()).isEqualTo("0.00");
        assertThat(checkedInOnly.operatingProfit()).isEqualTo("40.00");
    }

    @Test
    void roomOccupancyExcludesBlockedNightsAndRoomProfitExcludesWholePropertyExpenses() {
        YearMonth month = YearMonth.of(2026, 1);
        ReportOrderEntity checkedIn = order(1L, 1L, LocalDate.of(2026, 1, 3),
                LocalDate.of(2026, 1, 5), OrderStatus.CHECKED_IN,
                "50.00", "5.00", "0.00", "0.00");
        ReportBlockEntity block = new ReportBlockEntity();
        block.setRoomId(1L);
        block.setStartDate(LocalDate.of(2026, 1, 1));
        block.setEndDate(LocalDate.of(2026, 1, 3));
        ReportBlockEntity fullyBlocked = new ReportBlockEntity();
        fullyBlocked.setRoomId(3L);
        fullyBlocked.setStartDate(LocalDate.of(2026, 1, 1));
        fullyBlocked.setEndDate(LocalDate.of(2026, 2, 1));
        prepare(month, List.of(room(1L, "101"), room(2L, "102"), room(3L, "103")),
                List.of(checkedIn), List.of(block, fullyBlocked), new BigDecimal("10.00"));

        List<RoomReportDimensionItemVO> rooms = service.rooms(month, ReportScope.ALL, null).list();

        assertThat(rooms).hasSize(3);
        assertThat(rooms.get(0).roomProfit()).isEqualTo("45.00");
        assertThat(rooms.get(0).occupancyRate()).isEqualByComparingTo("0.0690");
        assertThat(rooms.get(1).roomProfit()).isEqualTo("0.00");
        assertThat(rooms.get(1).occupancyRate()).isEqualByComparingTo("0.0000");
        assertThat(rooms.get(2).occupancyRate()).isNull();

        JsonNode roomJson = objectMapper().valueToTree(rooms.get(2));
        assertThat(roomJson.has("occupancy_rate")).isTrue();
        assertThat(roomJson.get("occupancy_rate").isNull()).isTrue();
    }

    @Test
    void creatorReportIncludesAnExplicitNullIdForOrdersWithoutCreator() {
        YearMonth month = YearMonth.of(2026, 1);
        ReportOrderEntity order = order(1L, 1L, LocalDate.of(2026, 1, 5),
                LocalDate.of(2026, 1, 6), OrderStatus.PENDING_CONFIRM,
                "20.00", "0.00", "0.00", "0.00");
        prepare(month, List.of(room(1L, "101")), List.of(order), List.of(), new BigDecimal("0.00"));
        when(creatorMapper.findAll(null, null, null)).thenReturn(List.of());

        List<ReportDimensionItemVO> creators = service.creators(month, ReportScope.ALL, null).list();

        assertThat(creators).hasSize(1);
        assertThat(creators.get(0).id()).isNull();
        assertThat(creators.get(0).name()).isEqualTo("无主播订单");
        assertThat(creators.get(0).paidAmount()).isEqualTo("20.00");
        JsonNode creatorJson = objectMapper().valueToTree(creators.get(0));
        assertThat(creatorJson.has("id")).isTrue();
        assertThat(creatorJson.get("id").isNull()).isTrue();
    }


    @Test
    void dailyDashboardMatchesRoomAndSeparateNetAllocation() {
        LocalDate date = LocalDate.of(2026, 2, 2);
        DashboardRoomEntity booked = new DashboardRoomEntity();
        booked.setOrderId(1L); booked.setCheckIn(LocalDate.of(2026, 1, 31));
        booked.setOrderStatus("PENDING_CHECKIN");
        DashboardRoomEntity blocked = new DashboardRoomEntity(); blocked.setBlockId(3L);
        DashboardRoomEntity free = new DashboardRoomEntity();
        when(reportMapper.findDashboardRooms(date, CANCELLED_STATUS)).thenReturn(List.of(booked, blocked, free));
        ReportOrderEntity order = order(1L, 1L, LocalDate.of(2026, 1, 31),
                LocalDate.of(2026, 2, 3), OrderStatus.PENDING_CHECKIN, "100.01", "10.01", "3.01", "2.01");
        order.setNetAmount(new BigDecimal("84.98"));
        ReportOrderEntity ended = order(2L, 2L, LocalDate.of(2026, 2, 1),
                date, OrderStatus.COMPLETED, "99.00", "0.00", "0.00", "0.00");
        when(reportMapper.findMonthOrders(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 1), CANCELLED_STATUS))
                .thenReturn(List.of(order, ended));
        var result = service.dailyBusinessData(date);
        assertThat(result.occupiedRooms()).isEqualTo(2);
        assertThat(result.availableRooms()).isEqualTo(1);
        assertThat(result.occupancyRate()).isEqualTo("50.0%");
        assertThat(result.paidAmount()).isEqualTo("33.35");
        assertThat(result.platformFee()).isEqualTo("3.35");
        assertThat(result.platformCommission()).isEqualTo("1.01");
        assertThat(result.extraCommission()).isEqualTo("0.67");
        assertThat(result.netAmount()).isEqualTo("28.34");
    }

    @Test
    void dailyDashboardHandlesAllRoomsBlocked() {
        LocalDate date = LocalDate.of(2026, 2, 2);
        DashboardRoomEntity blocked = new DashboardRoomEntity(); blocked.setBlockId(1L);
        when(reportMapper.findDashboardRooms(date, CANCELLED_STATUS)).thenReturn(List.of(blocked));
        var result = service.dailyBusinessData(date);
        assertThat(result.occupancyRate()).isEqualTo("0.0%");
        assertThat(result.availableRooms()).isZero();
        assertThat(result.paidAmount()).isEqualTo("0.00");
    }

    private void prepare(
            YearMonth month,
            List<RoomEntity> rooms,
            List<ReportOrderEntity> orders,
            List<ReportBlockEntity> blocks,
            BigDecimal expenses) {
        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.plusMonths(1).atDay(1);
        when(roomMapper.findAll()).thenReturn(rooms);
        when(reportMapper.findMonthOrders(monthStart, monthEnd, CANCELLED_STATUS)).thenReturn(orders);
        when(reportMapper.findMonthBlocks(monthStart, monthEnd)).thenReturn(blocks);
        when(expenseMapper.sumBetween(monthStart, monthEnd.minusDays(1))).thenReturn(expenses);
    }

    private RoomEntity room(Long id, String number) {
        RoomEntity room = new RoomEntity();
        room.setId(id);
        room.setNumber(number);
        return room;
    }

    private ReportOrderEntity order(
            Long id,
            Long roomId,
            LocalDate checkIn,
            LocalDate checkOut,
            OrderStatus status,
            String paid,
            String platformFee,
            String platformCommission,
            String extraCommission) {
        ReportOrderEntity order = new ReportOrderEntity();
        order.setId(id);
        order.setRoomId(roomId);
        order.setChannelId(1L);
        order.setCheckIn(checkIn);
        order.setCheckOut(checkOut);
        order.setStatus(status.name());
        order.setPaidAmount(new BigDecimal(paid));
        order.setPlatformFee(new BigDecimal(platformFee));
        order.setPlatformCommission(new BigDecimal(platformCommission));
        order.setExtraCommission(new BigDecimal(extraCommission));
        return order;
    }

    private ObjectMapper objectMapper() {
        return new ObjectMapper()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .setDefaultPropertyInclusion(JsonInclude.Value.construct(
                        JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_NULL));
    }
}
