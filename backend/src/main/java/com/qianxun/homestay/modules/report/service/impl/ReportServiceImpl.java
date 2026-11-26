package com.qianxun.homestay.modules.report.service.impl;

import com.qianxun.homestay.modules.report.vo.RoomDateStatusVO;

import com.qianxun.homestay.modules.channel.mapper.ChannelMapper;
import com.qianxun.homestay.modules.creator.mapper.CreatorMapper;
import com.qianxun.homestay.modules.expense.mapper.ExpenseMapper;
import com.qianxun.homestay.modules.order.enums.OrderStatus;
import com.qianxun.homestay.modules.report.entity.DashboardRoomEntity;
import com.qianxun.homestay.modules.report.entity.ReportBlockEntity;
import com.qianxun.homestay.modules.report.entity.ReportOrderEntity;
import com.qianxun.homestay.modules.report.enums.ReportScope;
import com.qianxun.homestay.modules.report.mapper.ReportMapper;
import com.qianxun.homestay.modules.report.service.ReportService;
import com.qianxun.homestay.modules.report.vo.DashboardMonthSummaryVO;
import com.qianxun.homestay.modules.report.vo.DashboardRoomVO;
import com.qianxun.homestay.modules.report.vo.DashboardVO;
import com.qianxun.homestay.modules.report.vo.TodayBusinessDataVO;
import com.qianxun.homestay.modules.report.vo.ReportDimensionItemVO;
import com.qianxun.homestay.modules.report.vo.ReportDimensionVO;
import com.qianxun.homestay.modules.report.vo.ReportOverviewVO;
import com.qianxun.homestay.modules.report.vo.RoomReportDimensionItemVO;
import com.qianxun.homestay.modules.report.vo.RoomReportDimensionVO;
import com.qianxun.homestay.modules.room.entity.RoomEntity;
import com.qianxun.homestay.modules.room.mapper.RoomMapper;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 汇总订单金额快照、实际支出和封房夜晚，动态生成月度报表。 */
@Service
public class ReportServiceImpl implements ReportService {

    private static final int MONEY_SCALE = 2;
    private static final int RATE_SCALE = 4;
    private static final BigDecimal ZERO_AMOUNT = new BigDecimal("0.00");
    private static final String CANCELLED_STATUS = OrderStatus.CANCELLED.name();
    private static final String NO_CREATOR_NAME = "无主播订单";

    private final ReportMapper reportMapper;
    private final ExpenseMapper expenseMapper;
    private final RoomMapper roomMapper;
    private final ChannelMapper channelMapper;
    private final CreatorMapper creatorMapper;

    public ReportServiceImpl(
            ReportMapper reportMapper,
            ExpenseMapper expenseMapper,
            RoomMapper roomMapper,
            ChannelMapper channelMapper,
            CreatorMapper creatorMapper) {
        this.reportMapper = reportMapper;
        this.expenseMapper = expenseMapper;
        this.roomMapper = roomMapper;
        this.channelMapper = channelMapper;
        this.creatorMapper = creatorMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardVO dashboard(LocalDate date, YearMonth month) {
        List<DashboardRoomVO> roomCards = reportMapper.findDashboardRooms(date, CANCELLED_STATUS).stream()
                .map(room -> toDashboardRoom(room, date))
                .toList();
        MonthData monthData = loadMonth(month);
        ReportOverviewVO metrics = calculateOverview(monthData, ReportScope.ALL);
        DashboardMonthSummaryVO monthSummary = new DashboardMonthSummaryVO(
                metrics.unoccupiedOrderRevenue(),
                metrics.checkedInOrderRevenue(),
                metrics.platformFee(),
                metrics.platformCommission(),
                metrics.extraCommission(),
                metrics.netAmount(),
                metrics.expenseAmount(),
                metrics.operatingProfit(),
                metrics.occupancyRate());
        int occupiedRooms = (int) roomCards.stream().filter(room -> room.orderId() != null).count();
        return new DashboardVO(date, occupiedRooms, roomCards.size(), roomCards, monthSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomDateStatusVO> roomDateStatuses(LocalDate date) {
        // 连住夜晚的日历标记不包含订单状态，需要同时保留原状态供智能体准确解释。
        return reportMapper.findDashboardRooms(date, CANCELLED_STATUS).stream().map(room -> {
            DashboardRoomVO calendar = toDashboardRoom(room, date);
            return new RoomDateStatusVO(
                    calendar.roomNumber(), calendar.status(), room.getOrderStatus(),
                    calendar.guestName(), calendar.checkIn(), calendar.checkOut());
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TodayBusinessDataVO dailyBusinessData(LocalDate date) {
        // 房间分类与日看板一致，封房计入占用但不计入入住率分母。
        List<DashboardRoomVO> rooms = reportMapper.findDashboardRooms(date, CANCELLED_STATUS).stream()
                .map(room -> toDashboardRoom(room, date)).toList();
        int booked = (int) rooms.stream().filter(room -> room.orderId() != null).count();
        int blocked = (int) rooms.stream().filter(room -> "BLOCKED".equals(room.status())).count();
        int sellable = rooms.size() - blocked;
        String rate = sellable <= 0 ? "0.0%" : BigDecimal.valueOf(booked).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(sellable), 1, RoundingMode.HALF_UP).toPlainString() + "%";
        YearMonth month = YearMonth.from(date);
        List<ReportOrderEntity> orders = reportMapper.findMonthOrders(month.atDay(1),
                month.plusMonths(1).atDay(1), CANCELLED_STATUS);
        BigDecimal paid = ZERO_AMOUNT, fee = ZERO_AMOUNT, commission = ZERO_AMOUNT;
        BigDecimal extra = ZERO_AMOUNT, net = ZERO_AMOUNT;
        for (ReportOrderEntity order : orders) {
            if (CANCELLED_STATUS.equals(order.getStatus()) || date.isBefore(order.getCheckIn())
                    || !date.isBefore(order.getCheckOut())) continue;
            long nights = ChronoUnit.DAYS.between(order.getCheckIn(), order.getCheckOut());
            long index = ChronoUnit.DAYS.between(order.getCheckIn(), date);
            paid = paid.add(allocate(order.getPaidAmount(), nights, index));
            fee = fee.add(allocate(order.getPlatformFee(), nights, index));
            commission = commission.add(allocate(order.getPlatformCommission(), nights, index));
            extra = extra.add(allocate(order.getExtraCommission(), nights, index));
            // 日看板对净额单独分摊，不能以当日实付减费用替代。
            net = net.add(allocate(order.getNetAmount(), nights, index));
        }
        return new TodayBusinessDataVO(date, rooms.size(), booked + blocked,
                Math.max(0, rooms.size() - booked - blocked), rate,
                money(paid), money(fee), money(commission), money(extra), money(net));
    }

    @Override
    @Transactional(readOnly = true)
    public ReportOverviewVO overview(YearMonth month, ReportScope scope) {
        return calculateOverview(loadMonth(month), scope);
    }

    @Override
    @Transactional(readOnly = true)
    public ReportDimensionVO channels(YearMonth month, ReportScope scope, Long channelId) {
        List<Category> categories = channelMapper.findAll().stream()
                .filter(channel -> channelId == null || channel.getId().equals(channelId))
                .map(channel -> new Category(channel.getId(), channel.getName()))
                .toList();
        return calculateDimension(loadMonth(month), scope, categories, DimensionKind.CHANNEL);
    }

    @Override
    @Transactional(readOnly = true)
    public ReportDimensionVO creators(YearMonth month, ReportScope scope, Long creatorId) {
        List<Category> categories = new ArrayList<>();
        creatorMapper.findAll(null, null, null).stream()
                .filter(creator -> creatorId == null || creator.getId().equals(creatorId))
                .map(creator -> new Category(creator.getId(), creator.getName()))
                .forEach(categories::add);
        if (creatorId == null) {
            categories.add(new Category(null, NO_CREATOR_NAME));
        }
        return calculateDimension(loadMonth(month), scope, categories, DimensionKind.CREATOR);
    }

    @Override
    @Transactional(readOnly = true)
    public RoomReportDimensionVO rooms(YearMonth month, ReportScope scope, Long roomId) {
        List<Category> categories = roomMapper.findAll().stream()
                .filter(room -> roomId == null || room.getId().equals(roomId))
                .map(room -> new Category(room.getId(), room.getNumber()))
                .toList();
        MonthData data = loadMonth(month);
        DimensionData dimensionData = aggregateDimension(data, scope, categories, DimensionKind.ROOM);
        List<RoomReportDimensionItemVO> items = categories.stream()
                .map(category -> {
                    Metrics metrics = dimensionData.metrics().get(category.id());
                    long blocked = dimensionData.blockedNights().forRoom(category.id());
                    long denominator = month.lengthOfMonth() - blocked;
                    BigDecimal rate = occupancyRate(
                            dimensionData.checkedInNights().getOrDefault(category.id(), 0L), denominator);
                    return toRoomDimensionItem(category, metrics, rate);
                })
                .toList();
        return new RoomReportDimensionVO(month.toString(), scope, items);
    }

    private MonthData loadMonth(YearMonth month) {
        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.plusMonths(1).atDay(1);
        List<RoomEntity> rooms = roomMapper.findAll();
        List<ReportOrderEntity> orders = reportMapper.findMonthOrders(monthStart, monthEnd, CANCELLED_STATUS);
        List<ReportBlockEntity> blocks = reportMapper.findMonthBlocks(monthStart, monthEnd);
        BigDecimal expenseAmount = expenseMapper.sumBetween(monthStart, monthEnd.minusDays(1));
        return new MonthData(month, monthStart, monthEnd, rooms, orders, blocks, zeroIfNull(expenseAmount));
    }

    private ReportOverviewVO calculateOverview(MonthData data, ReportScope scope) {
        Metrics selected = new Metrics();
        Metrics notCheckedIn = new Metrics();
        Metrics checkedIn = new Metrics();
        long checkedInNights = 0;
        for (ReportOrderEntity order : data.orders()) {
            OrderStatus status = OrderStatus.valueOf(order.getStatus());
            Metrics orderMetrics = aggregateOrder(data, order);
            if (scope.includes(status)) {
                selected.add(orderMetrics);
            }
            if (ReportScope.NOT_CHECKED_IN.includes(status)) {
                notCheckedIn.add(orderMetrics);
            }
            if (ReportScope.CHECKED_IN.includes(status)) {
                checkedIn.add(orderMetrics);
                checkedInNights += monthNightCount(data, order);
            }
        }
        BlockedNights blockedNights = blockedNights(data);
        long denominator = calendarNights(data.month(), data.rooms().size()) - blockedNights.totalCount();
        return new ReportOverviewVO(
                data.month().toString(),
                scope,
                selected.orderCount,
                selected.nightCount,
                money(selected.paidAmount),
                money(selected.platformFee),
                money(selected.platformCommission),
                money(selected.extraCommission),
                money(selected.netAmount()),
                money(scope == ReportScope.CHECKED_IN ? ZERO_AMOUNT : notCheckedIn.paidAmount),
                money(scope == ReportScope.NOT_CHECKED_IN ? ZERO_AMOUNT : checkedIn.paidAmount),
                money(data.expenseAmount()),
                money(checkedIn.netAmount().subtract(data.expenseAmount())),
                occupancyRate(checkedInNights, denominator));
    }

    private ReportDimensionVO calculateDimension(
            MonthData data,
            ReportScope scope,
            List<Category> categories,
            DimensionKind kind) {
        DimensionData dimensionData = aggregateDimension(data, scope, categories, kind);
        List<ReportDimensionItemVO> items = categories.stream()
                .map(category -> toDimensionItem(category, dimensionData.metrics().get(category.id())))
                .toList();
        return new ReportDimensionVO(data.month().toString(), scope, items);
    }

    private DimensionData aggregateDimension(
            MonthData data, ReportScope scope, List<Category> categories, DimensionKind kind) {
        Map<Long, Metrics> groupedMetrics = new LinkedHashMap<>();
        for (Category category : categories) {
            groupedMetrics.put(category.id(), new Metrics());
        }
        Map<Long, Long> checkedInNights = new HashMap<>();
        for (ReportOrderEntity order : data.orders()) {
            Long groupId = groupId(order, kind);
            Metrics metrics = groupedMetrics.get(groupId);
            OrderStatus status = OrderStatus.valueOf(order.getStatus());
            if (metrics != null && scope.includes(status)) {
                metrics.add(aggregateOrder(data, order));
            }
            if (kind == DimensionKind.ROOM
                    && order.getRoomId().equals(groupId)
                    && ReportScope.CHECKED_IN.includes(status)) {
                checkedInNights.merge(groupId, monthNightCount(data, order), Long::sum);
            }
        }
        BlockedNights blockedNights = kind == DimensionKind.ROOM ? blockedNights(data) : null;
        return new DimensionData(groupedMetrics, checkedInNights, blockedNights);
    }

    private Metrics aggregateOrder(MonthData data, ReportOrderEntity order) {
        Metrics metrics = new Metrics();
        if (!order.getCheckIn().isBefore(data.monthStart()) && order.getCheckIn().isBefore(data.monthEnd())) {
            metrics.orderCount = 1;
        }
        long fullNights = ChronoUnit.DAYS.between(order.getCheckIn(), order.getCheckOut());
        LocalDate start = laterDate(order.getCheckIn(), data.monthStart());
        LocalDate end = earlierDate(order.getCheckOut(), data.monthEnd());
        for (LocalDate night = start; night.isBefore(end); night = night.plusDays(1)) {
            long nightIndex = ChronoUnit.DAYS.between(order.getCheckIn(), night);
            metrics.nightCount++;
            metrics.paidAmount = metrics.paidAmount.add(allocate(order.getPaidAmount(), fullNights, nightIndex));
            metrics.platformFee = metrics.platformFee.add(allocate(order.getPlatformFee(), fullNights, nightIndex));
            metrics.platformCommission = metrics.platformCommission.add(
                    allocate(order.getPlatformCommission(), fullNights, nightIndex));
            metrics.extraCommission = metrics.extraCommission.add(
                    allocate(order.getExtraCommission(), fullNights, nightIndex));
        }
        return metrics;
    }

    private DashboardRoomVO toDashboardRoom(DashboardRoomEntity room, LocalDate date) {
        String status;
        if (room.getOrderId() != null) {
            status = date.equals(room.getCheckIn()) ? room.getOrderStatus() : "OCCUPIED";
        } else if (room.getBlockId() != null) {
            status = "BLOCKED";
        } else {
            status = "AVAILABLE";
        }
        return new DashboardRoomVO(
                room.getRoomId(),
                room.getRoomNumber(),
                room.getRoomName(),
                status,
                room.getOrderId(),
                room.getGuestName(),
                room.getGuestCount(),
                room.getCheckIn(),
                room.getCheckOut(),
                room.getChannelName());
    }

    private ReportDimensionItemVO toDimensionItem(Category category, Metrics metrics) {
        return new ReportDimensionItemVO(
                category.id(),
                category.name(),
                metrics.orderCount,
                metrics.nightCount,
                money(metrics.paidAmount),
                money(metrics.platformFee),
                money(metrics.platformCommission),
                money(metrics.extraCommission),
                money(metrics.platformCommission.add(metrics.extraCommission)),
                money(metrics.netAmount()));
    }

    private RoomReportDimensionItemVO toRoomDimensionItem(
            Category category, Metrics metrics, BigDecimal occupancyRate) {
        ReportDimensionItemVO common = toDimensionItem(category, metrics);
        return new RoomReportDimensionItemVO(
                common.id(),
                common.name(),
                common.orderCount(),
                common.nightCount(),
                common.paidAmount(),
                common.platformFee(),
                common.platformCommission(),
                common.extraCommission(),
                common.totalCommission(),
                common.netAmount(),
                common.netAmount(),
                occupancyRate);
    }

    private BlockedNights blockedNights(MonthData data) {
        Map<Long, Set<LocalDate>> datesByRoom = new HashMap<>();
        for (ReportBlockEntity block : data.blocks()) {
            LocalDate start = laterDate(block.getStartDate(), data.monthStart());
            LocalDate end = earlierDate(block.getEndDate(), data.monthEnd());
            Set<LocalDate> dates = datesByRoom.computeIfAbsent(block.getRoomId(), ignored -> new HashSet<>());
            for (LocalDate night = start; night.isBefore(end); night = night.plusDays(1)) {
                dates.add(night);
            }
        }
        long total = datesByRoom.values().stream().mapToLong(Set::size).sum();
        return new BlockedNights(datesByRoom, total);
    }

    private long monthNightCount(MonthData data, ReportOrderEntity order) {
        LocalDate start = laterDate(order.getCheckIn(), data.monthStart());
        LocalDate end = earlierDate(order.getCheckOut(), data.monthEnd());
        return Math.max(0, ChronoUnit.DAYS.between(start, end));
    }

    private BigDecimal allocate(BigDecimal amount, long fullNights, long nightIndex) {
        if (fullNights <= 0) {
            return ZERO_AMOUNT;
        }
        BigInteger totalCents = zeroIfNull(amount).setScale(MONEY_SCALE, RoundingMode.UNNECESSARY)
                .movePointRight(MONEY_SCALE).toBigIntegerExact();
        BigInteger[] parts = totalCents.divideAndRemainder(BigInteger.valueOf(fullNights));
        BigInteger nightCents = parts[0];
        if (nightIndex == fullNights - 1) {
            // 契约规定平均分摊的分币尾差归订单最后一晚。
            nightCents = nightCents.add(parts[1]);
        }
        return new BigDecimal(nightCents).movePointLeft(MONEY_SCALE);
    }

    private BigDecimal occupancyRate(long occupiedNights, long availableNights) {
        if (availableNights <= 0) {
            return null;
        }
        return BigDecimal.valueOf(occupiedNights)
                .divide(BigDecimal.valueOf(availableNights), RATE_SCALE, RoundingMode.HALF_UP);
    }

    private long calendarNights(YearMonth month, int roomCount) {
        return (long) month.lengthOfMonth() * roomCount;
    }

    private Long groupId(ReportOrderEntity order, DimensionKind kind) {
        return switch (kind) {
            case CHANNEL -> order.getChannelId();
            case CREATOR -> order.getCreatorId();
            case ROOM -> order.getRoomId();
        };
    }

    private LocalDate laterDate(LocalDate left, LocalDate right) {
        return left.isAfter(right) ? left : right;
    }

    private LocalDate earlierDate(LocalDate left, LocalDate right) {
        return left.isBefore(right) ? left : right;
    }

    private BigDecimal zeroIfNull(BigDecimal amount) {
        return amount == null ? ZERO_AMOUNT : amount;
    }

    private String money(BigDecimal amount) {
        return zeroIfNull(amount).setScale(MONEY_SCALE, RoundingMode.HALF_UP).toPlainString();
    }

    private record MonthData(
            YearMonth month,
            LocalDate monthStart,
            LocalDate monthEnd,
            List<RoomEntity> rooms,
            List<ReportOrderEntity> orders,
            List<ReportBlockEntity> blocks,
            BigDecimal expenseAmount) {
    }

    private record Category(Long id, String name) {
    }

    private record BlockedNights(Map<Long, Set<LocalDate>> datesByRoom, long totalCount) {
        private long forRoom(Long roomId) {
            return datesByRoom.getOrDefault(roomId, Set.of()).size();
        }
    }

    private record DimensionData(
            Map<Long, Metrics> metrics,
            Map<Long, Long> checkedInNights,
            BlockedNights blockedNights) {
    }

    private enum DimensionKind {
        CHANNEL,
        CREATOR,
        ROOM
    }

    private static final class Metrics {
        private long orderCount;
        private long nightCount;
        private BigDecimal paidAmount = ZERO_AMOUNT;
        private BigDecimal platformFee = ZERO_AMOUNT;
        private BigDecimal platformCommission = ZERO_AMOUNT;
        private BigDecimal extraCommission = ZERO_AMOUNT;

        private void add(Metrics other) {
            orderCount += other.orderCount;
            nightCount += other.nightCount;
            paidAmount = paidAmount.add(other.paidAmount);
            platformFee = platformFee.add(other.platformFee);
            platformCommission = platformCommission.add(other.platformCommission);
            extraCommission = extraCommission.add(other.extraCommission);
        }

        private BigDecimal netAmount() {
            return paidAmount.subtract(platformFee).subtract(platformCommission).subtract(extraCommission);
        }
    }
}
