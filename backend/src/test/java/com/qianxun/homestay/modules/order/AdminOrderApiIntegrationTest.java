package com.qianxun.homestay.modules.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** 使用真实 MySQL 8 验证第三方订单接口、金额快照、房态冲突、状态机和积分奖励。 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "app.jwt.member-expiration-seconds=2592000",
        "app.jwt.admin-expiration-seconds=28800",
        "app.auth.default-avatar=/assets/avatar-default.png"
})
@AutoConfigureMockMvc
class AdminOrderApiIntegrationTest {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final String ADMIN_USERNAME = "order-test-admin";
    private static final String ADMIN_PASSWORD = "order-test-password";

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("qianxun_order_test")
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

    @DynamicPropertySource
    static void registerDatabaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    private long roomId;
    private long secondRoomId;
    private String adminToken;

    @BeforeEach
    void resetOrderData() throws Exception {
        jdbcTemplate.update("DELETE FROM points_log");
        jdbcTemplate.update("DELETE FROM order_amount");
        jdbcTemplate.update("DELETE FROM orders");
        jdbcTemplate.update("DELETE FROM booking_lock");
        jdbcTemplate.update("DELETE FROM room_block");
        jdbcTemplate.update("DELETE FROM creators");
        jdbcTemplate.update("DELETE FROM room");
        jdbcTemplate.update("DELETE FROM members");
        jdbcTemplate.update("DELETE FROM admin");
        jdbcTemplate.update(
                "INSERT INTO admin (username, password_hash, enabled) VALUES (?, ?, 1)",
                ADMIN_USERNAME, passwordEncoder.encode(ADMIN_PASSWORD));
        jdbcTemplate.update(
                "INSERT INTO room (number, name, floor, price, capacity, enabled) VALUES (?, ?, 1, ?, 4, 1)",
                "101", "枕澜大床房", "300.00");
        jdbcTemplate.update(
                "INSERT INTO room (number, name, floor, price, capacity, enabled) VALUES (?, ?, 2, ?, 4, 1)",
                "201", "听雨双床房", "360.00");
        roomId = jdbcTemplate.queryForObject("SELECT id FROM room WHERE number = '101'", Long.class);
        secondRoomId = jdbcTemplate.queryForObject("SELECT id FROM room WHERE number = '201'", Long.class);
        adminToken = loginAdminToken();
    }

    @Test
    void orderLifecycleKeepsMoneySnapshotAndRewardsOnce() throws Exception {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        LocalDate checkIn = today.plusDays(8);
        LocalDate checkOut = checkIn.plusDays(2);
        String response = mockMvc.perform(post("/api/admin/orders")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(createRequest(roomId, checkIn, checkOut, "PENDING_CONFIRM", "OTA-100", "13800000001")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.order_no").value(org.hamcrest.Matchers.startsWith("QX")))
                .andExpect(jsonPath("$.data.net_amount").value("285.00"))
                .andReturn().getResponse().getContentAsString();
        JsonNode created = objectMapper.readTree(response).path("data");
        long orderId = created.path("id").asLong();

        mockMvc.perform(get("/api/admin/orders")
                        .header("Authorization", bearer(adminToken))
                        .param("channel_id", "2")
                        .param("status", "PENDING_CONFIRM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.list[0].paid_amount").value("300.00"))
                .andExpect(jsonPath("$.data.list[0].platform_commission").value("0.00"))
                .andExpect(jsonPath("$.data.list[0].extra_commission").value("0.00"))
                .andExpect(jsonPath("$.data.list[0].expected_amount").doesNotExist())
                .andExpect(jsonPath("$.data.list[0].net_amount").value("285.00"));
        mockMvc.perform(get("/api/admin/orders")
                        .header("Authorization", bearer(adminToken))
                        .param("size", "0"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/admin/orders/{id}", orderId).header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.platform_order_no").value("OTA-100"))
                .andExpect(jsonPath("$.data.paid_amount").value("300.00"))
                .andExpect(jsonPath("$.data.platform_fee").value("15.00"))
                .andExpect(jsonPath("$.data.net_amount").value("285.00"));

        mockMvc.perform(put("/api/admin/orders/{id}", orderId)
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content("{\"paid_amount\":\"1.00\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(422));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT paid_amount FROM order_amount WHERE order_id = ?", java.math.BigDecimal.class, orderId))
                .isEqualByComparingTo("300.00");

        mockMvc.perform(post("/api/admin/orders/{id}/confirm", orderId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_CHECKIN"));
        mockMvc.perform(post("/api/admin/orders/{id}/confirm", orderId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/admin/orders/{id}/check-in", orderId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isUnprocessableEntity());

        LocalDate pastCheckIn = today.minusDays(2);
        LocalDate pastCheckOut = today;
        mockMvc.perform(put("/api/admin/orders/{id}", orderId)
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content("{\"check_in\":\"" + today.minusDays(2) + "\",\"check_out\":\""
                                + today.minusDays(1) + "\"}"))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(put("/api/admin/orders/{id}", orderId)
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content("{\"check_in\":\"" + pastCheckIn + "\",\"check_out\":\"" + pastCheckOut + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nights").value(2));

        mockMvc.perform(post("/api/admin/orders/{id}/check-in", orderId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CHECKED_IN"));
        mockMvc.perform(post("/api/admin/orders/{id}/check-out", orderId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        long points = jdbcTemplate.queryForObject(
                "SELECT points FROM members WHERE phone = '13800000001'", Long.class);
        long growthPoints = jdbcTemplate.queryForObject(
                "SELECT growth_points FROM members WHERE phone = '13800000001'", Long.class);
        assertThat(points).isEqualTo(300);
        assertThat(growthPoints).isEqualTo(300);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM points_log WHERE ref_no = ?", Integer.class, "ORDER_REWARD:" + orderId))
                .isEqualTo(1);

        mockMvc.perform(put("/api/admin/orders/{id}", orderId)
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content("{\"phone\":\"13800000002\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phone").value("13800000002"));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM members", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM points_log WHERE order_id = ?", Integer.class, orderId)).isEqualTo(1);
        mockMvc.perform(post("/api/admin/orders/{id}/cancel", orderId)
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isConflict());
    }

    @Test
    void dateConflictsPlatformDuplicateCancellationAndAuthorizationAreEnforced() throws Exception {
        LocalDate checkIn = LocalDate.now(BUSINESS_ZONE).plusDays(12);
        LocalDate checkOut = checkIn.plusDays(2);
        String firstRequest = createRequest(roomId, checkIn, checkOut, "PENDING_CONFIRM", "OTA-200", null);

        mockMvc.perform(post("/api/admin/orders")
                        .contentType("application/json")
                        .content(firstRequest))
                .andExpect(status().isUnauthorized());
        String memberToken = loginMemberToken("13800000003");
        mockMvc.perform(get("/api/admin/orders").header("Authorization", bearer(memberToken)))
                .andExpect(status().isForbidden());

        String firstResponse = mockMvc.perform(post("/api/admin/orders")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(firstRequest))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long firstId = objectMapper.readTree(firstResponse).path("data").path("id").asLong();

        mockMvc.perform(post("/api/admin/orders")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(createRequest(roomId, checkIn.plusDays(1), checkOut.plusDays(1),
                                "PENDING_CONFIRM", "OTA-201", null)))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/admin/orders")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(createRequest(secondRoomId, checkIn.plusDays(20), checkOut.plusDays(20),
                                "PENDING_CONFIRM", "OTA-200", null)))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/admin/orders")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(createRequest(roomId, checkOut, checkOut.plusDays(1),
                                "PENDING_CONFIRM", "OTA-202", null)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/orders/{id}/cancel", firstId)
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content("{\"reason\":\"客人改期\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
        mockMvc.perform(get("/api/admin/orders/{id}", firstId).header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.remark").value("取消原因：客人改期"))
                .andExpect(jsonPath("$.data.net_amount").value("285.00"));

        mockMvc.perform(post("/api/admin/orders")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(createRequest(roomId, checkIn, checkOut, "CANCELLED", "OTA-203", null)))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(post("/api/admin/orders")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(createRequest(roomId, checkIn, checkIn, "PENDING_CONFIRM", "OTA-204", null)))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(post("/api/admin/orders")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(createRequest(roomId, checkIn, checkOut, "PENDING_CONFIRM", "OTA-205", null)))
                .andExpect(status().isOk());
    }

    @Test
    void simultaneousCreatesForDifferentRoomsAllocateUniqueOrderNumbers() throws Exception {
        LocalDate checkIn = LocalDate.now(BUSINESS_ZONE).plusDays(25);
        LocalDate checkOut = checkIn.plusDays(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            List<Callable<String>> requests = List.of(
                    () -> createAndReadOrderNo(roomId, checkIn, checkOut, "OTA-300"),
                    () -> createAndReadOrderNo(secondRoomId, checkIn, checkOut, "OTA-301"));
            var responses = executor.invokeAll(requests);
            List<String> orderNos = responses.stream().map(result -> {
                try {
                    return result.get();
                } catch (Exception exception) {
                    throw new IllegalStateException("Concurrent order request failed", exception);
                }
            }).toList();
            assertThat(orderNos).hasSize(2).doesNotHaveDuplicates();
            assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders", Integer.class)).isEqualTo(2);

            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);
            LocalDate contestedCheckIn = checkIn.plusDays(2);
            Callable<Integer> contestedRequest = () -> {
                ready.countDown();
                if (!start.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Concurrent request gate timed out");
                }
                return mockMvc.perform(post("/api/admin/orders")
                                .header("Authorization", bearer(adminToken))
                                .contentType("application/json")
                                .content(createRequest(roomId, contestedCheckIn, contestedCheckIn.plusDays(1),
                                        "PENDING_CONFIRM", Thread.currentThread().getName(), null)))
                        .andReturn().getResponse().getStatus();
            };
            var first = executor.submit(contestedRequest);
            var second = executor.submit(contestedRequest);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(List.of(first.get(), second.get())).containsExactlyInAnyOrder(200, 409);
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM orders WHERE room_id = ? AND check_in = ?", Integer.class,
                    roomId, contestedCheckIn)).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void creatorFeesAreCalculatedAndHistoricalCompletedOrdersRewardImmediately() throws Exception {
        jdbcTemplate.update("INSERT INTO creators (name, channel_id, enabled) VALUES (?, 2, 1)", "阶段四达人");
        long creatorId = jdbcTemplate.queryForObject(
                "SELECT id FROM creators WHERE name = ?", Long.class, "阶段四达人");
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        LocalDate checkIn = today.plusDays(9);
        LocalDate checkOut = checkIn.plusDays(2);

        mockMvc.perform(post("/api/admin/orders")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(createRequest(roomId, checkIn, checkOut, "PENDING_CONFIRM", "OTA-399", null)
                                .replace("\"channel_id\":2", "\"channel_id\":1")))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(post("/api/admin/orders")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(creatorRequest(roomId, creatorId, checkIn.plusDays(6), checkOut.plusDays(6),
                                "200.00", "OTA-403", null).replace("\"channel_id\":2", "\"channel_id\":3")))
                .andExpect(status().isUnprocessableEntity());

        String missingExpectedAmount = creatorRequest(
                roomId, creatorId, checkIn, checkOut, null, "OTA-400", null);
        mockMvc.perform(post("/api/admin/orders")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(missingExpectedAmount))
                .andExpect(status().isUnprocessableEntity());

        String creatorResponse = mockMvc.perform(post("/api/admin/orders")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(creatorRequest(roomId, creatorId, checkIn.plusDays(3), checkOut.plusDays(3),
                                "200.00", "OTA-401", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.net_amount").value("200.00"))
                .andReturn().getResponse().getContentAsString();
        long creatorOrderId = objectMapper.readTree(creatorResponse).path("data").path("id").asLong();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT extra_commission FROM order_amount WHERE order_id = ?",
                java.math.BigDecimal.class, creatorOrderId))
                .isEqualByComparingTo("65.00");

        LocalDate pastCheckIn = today.minusDays(6);
        String completedResponse = mockMvc.perform(post("/api/admin/orders")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(createRequest(roomId, pastCheckIn, pastCheckIn.plusDays(2),
                                "COMPLETED", "OTA-402", "13800000004")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.net_amount").value("285.00"))
                .andReturn().getResponse().getContentAsString();
        long completedId = objectMapper.readTree(completedResponse).path("data").path("id").asLong();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT points FROM members WHERE phone = '13800000004'", Integer.class)).isEqualTo(300);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM points_log WHERE ref_no = ?", Integer.class, "ORDER_REWARD:" + completedId))
                .isEqualTo(1);
    }

    private String createAndReadOrderNo(long targetRoomId, LocalDate checkIn, LocalDate checkOut, String platformNo)
            throws Exception {
        String response = mockMvc.perform(post("/api/admin/orders")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(createRequest(targetRoomId, checkIn, checkOut, "PENDING_CONFIRM", platformNo, null)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("order_no").asText();
    }

    private String createRequest(
            long targetRoomId, LocalDate checkIn, LocalDate checkOut, String status, String platformNo, String phone) {
        String phoneField = phone == null ? "" : "\"phone\":\"" + phone + "\",";
        return "{\"channel_id\":2,\"room_id\":" + targetRoomId + ",\"platform_order_no\":\""
                + platformNo + "\",\"guest_name\":\"测试客人\"," + phoneField
                + "\"guest_count\":2,\"check_in\":\"" + checkIn + "\",\"check_out\":\"" + checkOut
                + "\",\"status\":\"" + status + "\",\"amount\":{\"paid_amount\":\"300.00\","
                + "\"platform_fee\":\"15.00\",\"platform_commission\":\"20.00\"}}";
    }

    private String creatorRequest(
            long targetRoomId,
            long creatorId,
            LocalDate checkIn,
            LocalDate checkOut,
            String expectedAmount,
            String platformNo,
            String phone) {
        String phoneField = phone == null ? "" : "\"phone\":\"" + phone + "\",";
        String expectedField = expectedAmount == null ? "" : ",\"expected_amount\":\"" + expectedAmount + "\"";
        return "{\"channel_id\":2,\"room_id\":" + targetRoomId + ",\"creator_id\":" + creatorId
                + ",\"platform_order_no\":\"" + platformNo + "\",\"guest_name\":\"测试客人\"," + phoneField
                + "\"check_in\":\"" + checkIn + "\",\"check_out\":\"" + checkOut
                + "\",\"status\":\"PENDING_CONFIRM\",\"amount\":{\"paid_amount\":\"300.00\","
                + "\"platform_fee\":\"15.00\",\"platform_commission\":\"20.00\"" + expectedField + "}}";
    }

    private String loginAdminToken() throws Exception {
        String response = mockMvc.perform(post("/api/admin/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"" + ADMIN_USERNAME + "\",\"password\":\""
                                + ADMIN_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("token").asText();
    }

    private String loginMemberToken(String phone) throws Exception {
        String response = mockMvc.perform(post("/api/app/auth/login")
                        .contentType("application/json")
                        .content("{\"phone\":\"" + phone + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("token").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
