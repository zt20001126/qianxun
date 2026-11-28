package com.qianxun.homestay.modules.availability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qianxun.homestay.modules.auth.WechatLoginTestSupport;
import com.qianxun.homestay.modules.auth.service.WechatIdentityService;
import com.zaxxer.hikari.HikariDataSource;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** 使用 MySQL 8 验证房态合并、日期边界、封房冲突和管理员权限。 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "app.jwt.member-expiration-seconds=2592000",
        "app.jwt.admin-expiration-seconds=2592000",
        "app.auth.default-avatar=/assets/avatar-default.png"
})
@AutoConfigureMockMvc
class AvailabilityApiIntegrationTest {

    @MockitoBean
    private WechatIdentityService wechatIdentityService;

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final String ADMIN_USERNAME = "availability-admin";
    private static final String ADMIN_PASSWORD = "availability-password";

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("qianxun_availability_test")
            .withUsername("qianxun")
            .withPassword("qianxun-test-password");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private HikariDataSource dataSource;

    @DynamicPropertySource
    static void registerDatabaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    private long roomId;
    private String adminToken;

    @BeforeEach
    void resetData() throws Exception {
        jdbcTemplate.update("DELETE FROM booking_lock");
        jdbcTemplate.update("DELETE FROM holiday_price_date");
        jdbcTemplate.update("DELETE FROM orders");
        jdbcTemplate.update("DELETE FROM room_block");
        jdbcTemplate.update("DELETE FROM room");
        jdbcTemplate.update("DELETE FROM admin");
        jdbcTemplate.update("DELETE FROM members");
        jdbcTemplate.update(
                "INSERT INTO admin (username, password_hash, enabled) VALUES (?, ?, 1)",
                ADMIN_USERNAME, passwordEncoder.encode(ADMIN_PASSWORD));
        jdbcTemplate.update(
                "INSERT INTO room (number, name, floor, price, capacity, enabled) VALUES (?, ?, 1, ?, 2, 1)",
                "101", "枕澜大床房", "100.00");
        roomId = jdbcTemplate.queryForObject("SELECT id FROM room WHERE number = '101'", Long.class);
        adminToken = loginAdminToken();
    }

    @Test
    void adminCalendarShowsOrderAndBlockStatesButHidesCancelledOrdersAndLiveLocks() throws Exception {
        YearMonth month = YearMonth.now(BUSINESS_ZONE).plusMonths(1);
        LocalDate checkIn = month.atDay(3);
        insertOrder("QXTESTCAL0001", checkIn, checkIn.plusDays(2), "PENDING_CONFIRM");
        insertOrder("QXTESTCAL0002", month.atDay(6), month.atDay(7), "CANCELLED");
        jdbcTemplate.update(
                "INSERT INTO room_block (room_id, start_date, end_date, reason) VALUES (?, ?, ?, ?)",
                roomId, month.atDay(8), month.atDay(10), "日历测试封房");
        long memberId = insertMember("13800000072");
        insertBookingLock(memberId, month.atDay(12), month.atDay(13), "PENDING",
                LocalDateTime.now(BUSINESS_ZONE).plusMinutes(10));

        mockMvc.perform(get("/api/admin/room-calendar").param("month", month.toString()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/room-calendar")
                        .header("Authorization", bearer(adminToken))
                        .param("month", "2026-13"))
                .andExpect(status().isBadRequest());

        String response = mockMvc.perform(get("/api/admin/room-calendar")
                        .header("Authorization", bearer(adminToken))
                        .param("month", month.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.month").value(month.toString()))
                .andExpect(jsonPath("$.data.rooms.length()").value(1))
                .andExpect(jsonPath("$.data.rooms[0].days.length()").value(month.lengthOfMonth()))
                .andExpect(jsonPath("$.data.rooms[0].days[2].status").value("PENDING_CONFIRM"))
                .andExpect(jsonPath("$.data.rooms[0].days[3].status").value("OCCUPIED"))
                .andExpect(jsonPath("$.data.rooms[0].days[5].status").value("AVAILABLE"))
                .andExpect(jsonPath("$.data.rooms[0].days[7].status").value("BLOCKED"))
                .andExpect(jsonPath("$.data.rooms[0].days[11].status").value("AVAILABLE"))
                .andReturn().getResponse().getContentAsString();

        JsonNode days = objectMapper.readTree(response).path("data").path("rooms").get(0).path("days");
        assertThat(days.get(2).path("order_id").asLong()).isPositive();
        assertThat(days.get(3).path("order_id").asLong()).isEqualTo(days.get(2).path("order_id").asLong());
        assertThat(days.get(5).has("order_id")).isFalse();
    }

    @Test
    void publicAvailabilityQueriesMergeOrdersAndOnlyLiveLocks() throws Exception {
        LocalDate firstNight = LocalDate.now(BUSINESS_ZONE).plusDays(5);
        insertOrder("QXTEST000001", firstNight, firstNight.plusDays(2), "PENDING_CONFIRM");
        insertOrder("QXTEST000002", firstNight.plusDays(2), firstNight.plusDays(3), "CANCELLED");

        long memberId = insertMember("13800000071");
        insertBookingLock(memberId, firstNight.plusDays(4), firstNight.plusDays(5), "PENDING",
                LocalDateTime.now(BUSINESS_ZONE).plusMinutes(10));
        insertBookingLock(memberId, firstNight.plusDays(6), firstNight.plusDays(7), "PENDING",
                LocalDateTime.now(BUSINESS_ZONE).minusMinutes(1));

        String month = firstNight.toString().substring(0, 7);
        String response = mockMvc.perform(get("/api/app/rooms/{id}/availability", roomId).param("month", month))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.room_id").value(roomId))
                .andExpect(jsonPath("$.data.month").value(month))
                .andExpect(jsonPath("$.data.unavailable_dates").isArray())
                .andReturn().getResponse().getContentAsString();
        JsonNode unavailableDates = objectMapper.readTree(response).path("data").path("unavailable_dates");
        String combinedDates = unavailableDates.toString();
        String finalMonth = firstNight.plusDays(6).toString().substring(0, 7);
        if (!month.equals(finalMonth)) {
            // 可订日历按月返回；测试日期跨月时同时读取下一月，验证有效和过期锁房。
            String nextResponse = mockMvc.perform(get("/api/app/rooms/{id}/availability", roomId).param("month", finalMonth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.month").value(finalMonth))
                    .andReturn().getResponse().getContentAsString();
            combinedDates += objectMapper.readTree(nextResponse).path("data").path("unavailable_dates").toString();
        }
        assertThat(combinedDates)
                .contains(firstNight.toString())
                .contains(firstNight.plusDays(1).toString())
                .contains(firstNight.plusDays(4).toString())
                .doesNotContain(firstNight.plusDays(2).toString())
                .doesNotContain(firstNight.plusDays(6).toString());

        mockMvc.perform(get("/api/app/rooms")
                        .param("check_in", firstNight.toString())
                        .param("check_out", firstNight.plusDays(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list.length()").value(0));

        LocalDate availableCheckIn = firstNight.plusDays(8);
        mockMvc.perform(post("/api/app/rooms/availability-check")
                        .contentType("application/json")
                        .content("{\"room_id\":" + roomId + ",\"check_in\":\"" + availableCheckIn
                                + "\",\"check_out\":\"" + availableCheckIn.plusDays(2) + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(true))
                .andExpect(jsonPath("$.data.nights").value(2))
                .andExpect(jsonPath("$.data.room_amount").value("200.00"));

        mockMvc.perform(post("/api/app/rooms/availability-check")
                        .contentType("application/json")
                        .content("{\"room_id\":" + roomId + ",\"check_in\":\"" + firstNight
                                + "\",\"check_out\":\"" + firstNight.plusDays(2) + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(false));
    }

    @Test
    void roomListAndAvailabilityUseStayNightsAndExcludeCheckoutAcrossMonths() throws Exception {
        LocalDate checkIn = YearMonth.from(LocalDate.now(BUSINESS_ZONE).plusDays(1)).atEndOfMonth();
        LocalDate holiday = checkIn.plusDays(1);
        LocalDate checkOut = checkIn.plusDays(2);
        jdbcTemplate.update("UPDATE room SET holiday_price = 168.50 WHERE id = ?", roomId);
        jdbcTemplate.update("""
                INSERT INTO room (number, name, floor, price, capacity, enabled)
                VALUES ('102', '双床房', 1, 120.00, 2, 1)
                """);
        LocalDate saturday = LocalDate.now(BUSINESS_ZONE)
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
        mockMvc.perform(get("/api/app/rooms").param("check_in", saturday.toString())
                        .param("check_out", saturday.plusDays(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list[0].room_amount").value("100.00"));

        String selectedDates = objectMapper.writeValueAsString(Map.of(
                "month", YearMonth.from(holiday).toString(),
                "add_dates", List.of(holiday, checkOut),
                "remove_dates", List.of()));
        mockMvc.perform(put("/api/admin/holiday-price-dates").header("Authorization", bearer(adminToken))
                        .contentType("application/json").content(selectedDates))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dates.length()").value(2));

        mockMvc.perform(get("/api/app/rooms").param("check_in", checkIn.toString()).param("check_out", checkOut.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list[0].price").value("100.00"))
                .andExpect(jsonPath("$.data.list[0].room_amount").value("268.50"))
                .andExpect(jsonPath("$.data.list[1].room_amount").value("240.00"));

        String body = objectMapper.writeValueAsString(Map.of(
                "room_id", roomId, "check_in", checkIn, "check_out", checkOut));
        mockMvc.perform(post("/api/app/rooms/availability-check").contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nights").value(2))
                .andExpect(jsonPath("$.data.room_amount").value("268.50"));

        mockMvc.perform(get("/api/app/rooms").param("check_in", holiday.toString())
                        .param("check_out", checkOut.plusDays(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list[0].room_amount").value("337.00"));
    }

    @Test
    void roomQuoteRejectsAmountsAboveThePaymentSnapshotLimit() throws Exception {
        LocalDate checkIn = LocalDate.now(BUSINESS_ZONE).plusDays(1);
        LocalDate checkOut = checkIn.plusDays(2);
        jdbcTemplate.update("UPDATE room SET holiday_price = 9999999999.99 WHERE id = ?", roomId);
        jdbcTemplate.update("INSERT INTO holiday_price_date (holiday_date) VALUES (?), (?)", checkIn, checkIn.plusDays(1));
        String body = objectMapper.writeValueAsString(Map.of(
                "room_id", roomId, "check_in", checkIn, "check_out", checkOut));

        mockMvc.perform(post("/api/app/rooms/availability-check").contentType("application/json").content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("住宿房费超过可预订金额上限"));
    }

    @Test
    void appRangeChecksRejectPastAndOutOfWindowDatesButAllowCheckoutBoundary() throws Exception {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        LocalDate checkIn = today.plusDays(58);
        LocalDate lastDate = today.plusDays(59);

        mockMvc.perform(post("/api/app/rooms/availability-check")
                        .contentType("application/json")
                        .content("{\"room_id\":" + roomId + ",\"check_in\":\"" + checkIn
                                + "\",\"check_out\":\"" + lastDate + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nights").value(1));

        mockMvc.perform(post("/api/app/rooms/availability-check")
                        .contentType("application/json")
                        .content("{\"room_id\":" + roomId + ",\"check_in\":\"" + today.minusDays(1)
                                + "\",\"check_out\":\"" + today + "\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(422));

        mockMvc.perform(get("/api/app/rooms")
                        .param("check_in", checkIn.toString())
                        .param("check_out", today.plusDays(60).toString()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(422));

        mockMvc.perform(post("/api/app/rooms/availability-check")
                        .contentType("application/json")
                        .content("{\"room_id\":" + roomId + ",\"check_in\":\"" + today.plusDays(2)
                                + "\",\"check_out\":\"" + today.plusDays(2) + "\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void adminRoomBlocksRequireAdminAndRejectOrderAndLiveLockConflicts() throws Exception {
        LocalDate start = LocalDate.now(BUSINESS_ZONE).plusDays(10);
        insertOrder("QXTEST000003", start, start.plusDays(2), "PENDING_CHECKIN");
        long memberId = insertMember("13800000072");
        insertBookingLock(memberId, start.plusDays(3), start.plusDays(5), "PENDING",
                LocalDateTime.now(BUSINESS_ZONE).plusMinutes(10));

        mockMvc.perform(get("/api/admin/room-blocks"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
        String memberToken = loginMemberToken("13800000073");
        mockMvc.perform(get("/api/admin/room-blocks").header("Authorization", bearer(memberToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        String orderConflict = blockRequest(start, start.plusDays(1));
        mockMvc.perform(post("/api/admin/room-blocks")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(orderConflict))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));

        String lockConflict = blockRequest(start.plusDays(3), start.plusDays(4));
        mockMvc.perform(post("/api/admin/room-blocks")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(lockConflict))
                .andExpect(status().isConflict());

        LocalDate blockStart = start.plusDays(6);
        LocalDate blockEnd = start.plusDays(8);
        String createResponse = mockMvc.perform(post("/api/admin/room-blocks")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(blockRequest(blockStart, blockEnd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.room_id").value(roomId))
                .andExpect(jsonPath("$.data.start_date").value(blockStart.toString()))
                .andReturn().getResponse().getContentAsString();
        long blockId = objectMapper.readTree(createResponse).path("data").path("id").asLong();

        mockMvc.perform(post("/api/admin/room-blocks")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(blockRequest(blockStart.plusDays(1), blockEnd.plusDays(1))))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/admin/room-blocks")
                        .header("Authorization", bearer(adminToken))
                        .param("room_id", String.valueOf(roomId))
                        .param("month", blockStart.toString().substring(0, 7)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list.length()").value(1));

        mockMvc.perform(put("/api/admin/room-blocks/{id}", blockId)
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(blockRequest(start, start.plusDays(1))))
                .andExpect(status().isConflict());

        LocalDate adjustedStart = start.plusDays(8);
        LocalDate adjustedEnd = start.plusDays(10);
        mockMvc.perform(put("/api/admin/room-blocks/{id}", blockId)
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(blockRequest(adjustedStart, adjustedEnd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.end_date").value(adjustedEnd.toString()));

        mockMvc.perform(delete("/api/admin/room-blocks/{id}", blockId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/admin/room-blocks/{id}", blockId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/app/rooms/availability-check")
                        .contentType("application/json")
                        .content("{\"room_id\":" + roomId + ",\"check_in\":\"" + adjustedStart
                                + "\",\"check_out\":\"" + adjustedEnd + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(true));
    }

    @Test
    void concurrentRoomBlocksForTheSameNightAreSerializedByTheRoomRowLock() throws Exception {
        LocalDate start = LocalDate.now(BUSINESS_ZONE).plusDays(25);
        String request = blockRequest(start, start.plusDays(2));
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch begin = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> first = executor.submit(() -> createBlockAfterBarrier(ready, begin, request));
            Future<Integer> second = executor.submit(() -> createBlockAfterBarrier(ready, begin, request));
            ready.await();
            begin.countDown();
            assertThat(List.of(first.get(), second.get())).containsExactlyInAnyOrder(200, 409);
            assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM room_block", Integer.class)).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void updateRechecksConflictsCommittedWhileWaitingForTheRoomLock() throws Exception {
        LocalDate existingStart = LocalDate.now(BUSINESS_ZONE).plusDays(20);
        LocalDate existingEnd = existingStart.plusDays(2);
        String createResponse = mockMvc.perform(post("/api/admin/room-blocks")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(blockRequest(existingStart, existingEnd)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long blockId = objectMapper.readTree(createResponse).path("data").path("id").asLong();

        LocalDate competingStart = LocalDate.now(BUSINESS_ZONE).plusDays(30);
        LocalDate competingEnd = competingStart.plusDays(2);
        CountDownLatch roomLocked = new CountDownLatch(1);
        CountDownLatch allowCommit = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> competingWriter = executor.submit(() -> new TransactionTemplate(transactionManager).execute(status -> {
                jdbcTemplate.queryForObject("SELECT id FROM room WHERE id = ? FOR UPDATE", Long.class, roomId);
                roomLocked.countDown();
                await(allowCommit);
                jdbcTemplate.update(
                        "INSERT INTO room_block (room_id, start_date, end_date, reason) VALUES (?, ?, ?, ?)",
                        roomId, competingStart, competingEnd, "并发封房");
                return null;
            }));
            assertThat(roomLocked.await(5, TimeUnit.SECONDS)).isTrue();

            CountDownLatch updateStarted = new CountDownLatch(1);
            Future<Integer> updateResponse = executor.submit(() -> {
                updateStarted.countDown();
                return mockMvc.perform(put("/api/admin/room-blocks/{id}", blockId)
                                .header("Authorization", bearer(adminToken))
                                .contentType("application/json")
                                .content(blockRequest(competingStart, competingEnd)))
                        .andReturn().getResponse().getStatus();
            });
            assertThat(updateStarted.await(5, TimeUnit.SECONDS)).isTrue();
            awaitSecondJdbcConnection();
            allowCommit.countDown();

            competingWriter.get(5, TimeUnit.SECONDS);
            assertThat(updateResponse.get(5, TimeUnit.SECONDS)).isEqualTo(409);
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM room_block WHERE room_id = ? AND start_date = ? AND end_date = ?",
                    Integer.class, roomId, competingStart, competingEnd)).isEqualTo(1);
        } finally {
            allowCommit.countDown();
            executor.shutdownNow();
        }
    }

    private long insertMember(String phone) {
        jdbcTemplate.update("INSERT INTO members (phone, nickname, avatar) VALUES (?, ?, ?)",
                phone, phone.substring(phone.length() - 4), "/assets/avatar-default.png");
        return jdbcTemplate.queryForObject("SELECT id FROM members WHERE phone = ?", Long.class, phone);
    }

    private void insertOrder(String orderNo, LocalDate checkIn, LocalDate checkOut, String status) {
        jdbcTemplate.update(
                "INSERT INTO orders (order_no, room_id, channel_id, check_in, check_out, status) "
                        + "VALUES (?, ?, 2, ?, ?, ?)",
                orderNo, roomId, checkIn, checkOut, status);
    }

    private void insertBookingLock(
            long memberId, LocalDate checkIn, LocalDate checkOut, String status, LocalDateTime expiresAt) {
        jdbcTemplate.update(
                "INSERT INTO booking_lock (member_id, room_id, check_in, check_out, guest_name, phone, guest_count, "
                        + "room_amount, discount_amount, points_used, amount, status, expires_at) "
                        + "VALUES (?, ?, ?, ?, '测试联系人', '13800000000', 1, 100.00, 0.00, 0, 100.00, ?, ?)",
                memberId, roomId, checkIn, checkOut, status, expiresAt);
    }

    private String blockRequest(LocalDate startDate, LocalDate endDate) {
        return "{\"room_id\":" + roomId + ",\"start_date\":\"" + startDate
                + "\",\"end_date\":\"" + endDate + "\",\"reason\":\"接口测试\"}";
    }

    private int createBlockAfterBarrier(CountDownLatch ready, CountDownLatch begin, String request) throws Exception {
        ready.countDown();
        begin.await();
        return mockMvc.perform(post("/api/admin/room-blocks")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(request))
                .andReturn().getResponse().getStatus();
    }

    private void awaitSecondJdbcConnection() throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            if (dataSource.getHikariPoolMXBean().getActiveConnections() >= 2) {
                // 一条连接持有房间锁，第二条是更新请求；留出时间完成初始记录读取并进入锁等待。
                Thread.sleep(100);
                return;
            }
            Thread.sleep(25);
        }
        throw new AssertionError("更新请求未取得数据库连接");
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("并发封房同步超时");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("并发封房同步被中断", exception);
        }
    }

    private String loginAdminToken() throws Exception {
        String response = mockMvc.perform(post("/api/admin/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"" + ADMIN_USERNAME + "\",\"password\":\"" + ADMIN_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("token").asText();
    }

    private String loginMemberToken(String phone) throws Exception {
        WechatLoginTestSupport.stub(wechatIdentityService, phone);
        String response = mockMvc.perform(post("/api/app/auth/login")
                        .contentType("application/json")
                        .content(WechatLoginTestSupport.request(phone)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("token").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
