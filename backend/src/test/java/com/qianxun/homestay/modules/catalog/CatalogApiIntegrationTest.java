package com.qianxun.homestay.modules.catalog;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.qianxun.homestay.modules.auth.WechatLoginTestSupport;
import com.qianxun.homestay.modules.auth.service.WechatIdentityService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** 使用独立 MySQL 8 测试客房、渠道和达人接口的契约、权限与状态维护。 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "app.jwt.member-expiration-seconds=2592000",
        "app.jwt.admin-expiration-seconds=2592000",
        "app.auth.default-avatar=/assets/avatar-default.png"
})
@AutoConfigureMockMvc
class CatalogApiIntegrationTest {

    @MockitoBean
    private WechatIdentityService wechatIdentityService;

    private static final String ADMIN_USERNAME = "stage-two-admin";
    private static final String ADMIN_PASSWORD = "stage-two-password";

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("qianxun_catalog_test")
            .withUsername("qianxun")
            .withPassword("qianxun-test-password");

    @Container
    private static final org.testcontainers.containers.GenericContainer<?> REDIS =
            new org.testcontainers.containers.GenericContainer<>("redis:7.4.7-alpine").withExposedPorts(6379);

    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    private com.qianxun.homestay.modules.room.mapper.RoomMapper roomMapper;
    @Autowired
    private com.qianxun.homestay.common.cache.DisplayCacheService cache;
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
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    private String adminToken;

    @BeforeEach
    void resetStageTwoData() throws Exception {
        jdbcTemplate.update("DELETE FROM creators");
        jdbcTemplate.update("DELETE FROM holiday_price_date");
        jdbcTemplate.update("DELETE FROM room");
        cache.invalidateAfterCommit("rooms");
        jdbcTemplate.update("UPDATE channels SET enabled = 1");
        jdbcTemplate.update("DELETE FROM admin");
        jdbcTemplate.update("INSERT INTO admin (username, password_hash, enabled) VALUES (?, ?, 1)",
                ADMIN_USERNAME, passwordEncoder.encode(ADMIN_PASSWORD));
        adminToken = loginAdminToken();
    }

    @Test
    void publicRoomListAndDetailReturnOnlyEnabledRooms() throws Exception {
        long availableRoomId = insertRoom("101", "枕澜大床房", 1, "198.00", true);
        long disabledRoomId = insertRoom("102", "浅溪双床房", 1, "218.00", false);

        org.mockito.Mockito.clearInvocations(roomMapper);
        mockMvc.perform(get("/api/app/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list.length()").value(1))
                .andExpect(jsonPath("$.data.list[0].id").value(availableRoomId))
                .andExpect(jsonPath("$.data.list[0].number").value("101"))
                .andExpect(jsonPath("$.data.list[0].price").value("198.00"))
                .andExpect(jsonPath("$.data.list[0].images[0]").value("https://images.example/101.jpg"));

        mockMvc.perform(get("/api/app/rooms/{id}", availableRoomId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("枕澜大床房"))
                .andExpect(jsonPath("$.data.facilities[0]").value("地暖"));

        org.mockito.Mockito.verify(roomMapper, org.mockito.Mockito.times(1)).findEnabled();
        mockMvc.perform(get("/api/app/rooms/{id}", disabledRoomId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));

        LocalDate checkIn = LocalDate.now(ZoneId.of("Asia/Shanghai")).plusDays(1);
        LocalDate checkOut = checkIn.plusDays(2);
        mockMvc.perform(get("/api/app/rooms").param("check_in", checkIn.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(get("/api/app/rooms")
                        .param("check_in", checkIn.toString())
                        .param("check_out", checkOut.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list.length()").value(1))
                .andExpect(jsonPath("$.data.list[0].available").value(true));

        mockMvc.perform(get("/api/app/rooms")
                        .param("check_in", checkIn.minusDays(2).toString())
                        .param("check_out", checkIn.minusDays(1).toString()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(422));
    }

    @Test
    void roomHolidayPriceCanBeSetPreservedByLegacyUpdateAndCleared() throws Exception {
        long roomId = insertRoom("101", "101 房间", 1, "198.00", true);
        String ordinaryFields = """
                {"number":"101","name":"水乡客房","floor":1,"price":"199.00","capacity":2}
                """;
        ObjectNode update = objectMapper.readValue(ordinaryFields, ObjectNode.class);
        update.put("holiday_price", "-1.00");
        mockMvc.perform(put("/api/admin/rooms/{id}", roomId)
                        .header("Authorization", bearer(adminToken)).contentType("application/json").content(update.toString()))
                .andExpect(status().isBadRequest());

        update.put("holiday_price", "299.50");
        mockMvc.perform(put("/api/admin/rooms/{id}", roomId)
                        .header("Authorization", bearer(adminToken)).contentType("application/json").content(update.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.price").value("199.00"))
                .andExpect(jsonPath("$.data.holiday_price").value("299.50"));

        mockMvc.perform(put("/api/admin/rooms/{id}", roomId)
                        .header("Authorization", bearer(adminToken)).contentType("application/json").content(ordinaryFields))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.holiday_price").value("299.50"));

        update.putNull("holiday_price");
        mockMvc.perform(put("/api/admin/rooms/{id}", roomId)
                        .header("Authorization", bearer(adminToken)).contentType("application/json").content(update.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.holiday_price").value(nullValue()));
    }

    @Test
    void administratorsCanScheduleAndCancelDatesAheadOfTheBookingWindow() throws Exception {
        String path = "/api/admin/holiday-price-dates";
        YearMonth month = YearMonth.now(ZoneId.of("Asia/Shanghai")).plusMonths(3);
        LocalDate first = month.atDay(1);
        LocalDate second = first.plusDays(1);
        String add = objectMapper.writeValueAsString(Map.of(
                "month", month.toString(), "add_dates", List.of(first), "remove_dates", List.of()));

        mockMvc.perform(get(path).param("month", month.toString()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put(path).header("Authorization", bearer(loginMemberToken("13800000031")))
                        .contentType("application/json").content(add))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(path).param("month", "2026-13").header("Authorization", bearer(adminToken)))
                .andExpect(status().isBadRequest());

        for (int attempt = 0; attempt < 2; attempt++) {
            mockMvc.perform(put(path).header("Authorization", bearer(adminToken)).contentType("application/json").content(add))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.dates.length()").value(1))
                    .andExpect(jsonPath("$.data.dates[0]").value(first.toString()));
        }

        String invalid = objectMapper.writeValueAsString(Map.of(
                "month", month.toString(), "add_dates", List.of(second, month.plusMonths(1).atDay(1)), "remove_dates", List.of()));
        mockMvc.perform(put(path).header("Authorization", bearer(adminToken)).contentType("application/json").content(invalid))
                .andExpect(status().isBadRequest());
        String conflict = objectMapper.writeValueAsString(Map.of(
                "month", month.toString(), "add_dates", List.of(first), "remove_dates", List.of(first)));
        mockMvc.perform(put(path).header("Authorization", bearer(adminToken)).contentType("application/json").content(conflict))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(path).param("month", month.toString()).header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dates.length()").value(1))
                .andExpect(jsonPath("$.data.dates[0]").value(first.toString()));

        String change = objectMapper.writeValueAsString(Map.of(
                "month", month.toString(), "add_dates", List.of(second), "remove_dates", List.of(first)));
        mockMvc.perform(put(path).header("Authorization", bearer(adminToken)).contentType("application/json").content(change))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dates.length()").value(1))
                .andExpect(jsonPath("$.data.dates[0]").value(second.toString()));
        mockMvc.perform(get(path).param("month", month.toString()).header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dates.length()").value(1))
                .andExpect(jsonPath("$.data.dates[0]").value(second.toString()));
    }

    @Test
    void adminRoomCrudEnforcesRoleFixedRoomNumbersUniquenessAndListingState() throws Exception {
        long existingRoomId = insertRoom("101", "101 房间", 1, "198.00", true);
        jdbcTemplate.update("UPDATE room SET images = JSON_ARRAY(?, ?) WHERE id = ?",
                "https://images.example/101.jpg", "https://images.example/detail.jpg", existingRoomId);

        mockMvc.perform(get("/api/admin/rooms"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        String memberToken = loginMemberToken("13800000031");
        mockMvc.perform(get("/api/admin/rooms").header("Authorization", bearer(memberToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        mockMvc.perform(get("/api/admin/rooms").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list.length()").value(1))
                .andExpect(jsonPath("$.data.list[0].id").value(existingRoomId));

        String createRequest = """
                {
                  "number":"102",
                  "name":"浅溪双床房",
                  "floor":1,
                  "price":"218.00",
                  "capacity":2,
                  "bed_info":"单人床 1.5m × 2",
                  "area":30.00,
                  "facilities":["地暖","独立卫浴"],
                  "images":["https://images.example/102.jpg","https://images.example/detail.jpg"],
                  "description":"双床客房"
                }
                """;
        mockMvc.perform(post("/api/admin/rooms")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(createRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.number").value("102"))
                .andExpect(jsonPath("$.data.price").value("218.00"))
                .andExpect(jsonPath("$.data.enabled").value(true));

        mockMvc.perform(post("/api/admin/rooms")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(createRequest.replace("102", "104")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(422));

        mockMvc.perform(post("/api/admin/rooms")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(createRequest.replace("102", "101")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));

        String updateRequest = createRequest.replace("102", "101")
                .replace("浅溪双床房", "枕澜雅致大床房")
                .replace("218.00", "228.00")
                .replace("\"https://images.example/101.jpg\",\"https://images.example/detail.jpg\"",
                        "\"https://images.example/detail.jpg\",\"https://images.example/101.jpg\"");
        // 先读取小程序资料，确认重新排序后也能读到更新后的展示数据。
        mockMvc.perform(get("/api/app/rooms/{id}", existingRoomId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.images[0]").value("https://images.example/101.jpg"))
                .andExpect(jsonPath("$.data.images[1]").value("https://images.example/detail.jpg"));
        mockMvc.perform(put("/api/admin/rooms/{id}", existingRoomId)
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(updateRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("枕澜雅致大床房"))
                .andExpect(jsonPath("$.data.price").value("228.00"))
                .andExpect(jsonPath("$.data.images[0]").value("https://images.example/detail.jpg"));

        mockMvc.perform(get("/api/app/rooms/{id}", existingRoomId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.images[0]").value("https://images.example/detail.jpg"))
                .andExpect(jsonPath("$.data.images[1]").value("https://images.example/101.jpg"));
        mockMvc.perform(get("/api/app/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list[0].number").value("101"))
                .andExpect(jsonPath("$.data.list[0].images[0]").value("https://images.example/detail.jpg"));

        mockMvc.perform(put("/api/admin/rooms/{id}/enabled", existingRoomId)
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content("{\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false));
        mockMvc.perform(put("/api/admin/rooms/{id}/enabled", existingRoomId)
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content("{\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false));
        mockMvc.perform(get("/api/app/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list.length()").value(1))
                .andExpect(jsonPath("$.data.list[0].number").value("102"));
    }

    @Test
    void fixedChannelsCanBeListedAndToggledIdempotently() throws Exception {
        mockMvc.perform(get("/api/admin/channels"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        mockMvc.perform(get("/api/admin/channels").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list.length()").value(7))
                .andExpect(jsonPath("$.data.list[0].id").value(1))
                .andExpect(jsonPath("$.data.list[0].name").value("小程序"))
                .andExpect(jsonPath("$.data.list[6].name").value("途家"));

        mockMvc.perform(put("/api/admin/channels/2/enabled")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content("{\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false));

        mockMvc.perform(put("/api/admin/channels/2/enabled")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content("{\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false));

        mockMvc.perform(put("/api/admin/channels/999/enabled")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content("{\"enabled\":true}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void creatorCrudFiltersBySnakeCaseChannelIdAndPreservesDisabledHistory() throws Exception {
        String createRequest = "{\"name\":\"南浔旅拍\",\"channel_id\":2}";
        String createResponse = mockMvc.perform(post("/api/admin/creators")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(createRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("南浔旅拍"))
                .andExpect(jsonPath("$.data.channel_id").value(2))
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andReturn().getResponse().getContentAsString();
        long creatorId = objectMapper.readTree(createResponse).path("data").path("id").asLong();

        mockMvc.perform(get("/api/admin/creators")
                        .header("Authorization", bearer(adminToken))
                        .param("channel_id", "2")
                        .param("enabled", "true")
                        .param("name", "旅拍"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list.length()").value(1))
                .andExpect(jsonPath("$.data.list[0].id").value(creatorId));

        mockMvc.perform(get("/api/admin/creators/{id}", creatorId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.channel_id").value(2));

        mockMvc.perform(put("/api/admin/creators/{id}", creatorId)
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content("{\"name\":\"江南摄影\",\"channel_id\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("江南摄影"))
                .andExpect(jsonPath("$.data.channel_id").value(3));

        mockMvc.perform(put("/api/admin/creators/{id}/enabled", creatorId)
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content("{\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false));

        mockMvc.perform(get("/api/admin/creators")
                        .header("Authorization", bearer(adminToken))
                        .param("channel_id", "3")
                        .param("enabled", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list.length()").value(1));

        mockMvc.perform(post("/api/admin/creators")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content("{\"name\":\"无效渠道达人\",\"channel_id\":999}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    private long insertRoom(String number, String name, int floor, String price, boolean enabled) {
        jdbcTemplate.update("""
                INSERT INTO room (number, name, floor, price, capacity, bed_info, facilities, images, description, enabled)
                VALUES (?, ?, ?, ?, 2, '大床 1.8m', '["地暖"]', '["https://images.example/101.jpg"]', '测试房间', ?)
                """, number, name, floor, price, enabled);
        return jdbcTemplate.queryForObject("SELECT id FROM room WHERE number = ?", Long.class, number);
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
