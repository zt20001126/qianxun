package com.qianxun.homestay.modules.catalog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.ZoneId;
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

    private static final String ADMIN_USERNAME = "stage-two-admin";
    private static final String ADMIN_PASSWORD = "stage-two-password";

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("qianxun_catalog_test")
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

    private String adminToken;

    @BeforeEach
    void resetStageTwoData() throws Exception {
        jdbcTemplate.update("DELETE FROM creators");
        jdbcTemplate.update("DELETE FROM room");
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
    void adminRoomCrudEnforcesRoleFixedRoomNumbersUniquenessAndListingState() throws Exception {
        long existingRoomId = insertRoom("101", "101 房间", 1, "198.00", true);

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
                  "images":["https://images.example/102.jpg"],
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
                .replace("218.00", "228.00");
        mockMvc.perform(put("/api/admin/rooms/{id}", existingRoomId)
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(updateRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("枕澜雅致大床房"))
                .andExpect(jsonPath("$.data.price").value("228.00"));

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
