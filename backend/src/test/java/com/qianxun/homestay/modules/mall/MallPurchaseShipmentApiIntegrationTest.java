package com.qianxun.homestay.modules.mall;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qianxun.homestay.common.security.service.JwtService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** 使用真实 MySQL 8 验证会员购买至管理员发货、完成的 HTTP 全链路。 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "app.channel.mini-program-id=1"
})
@AutoConfigureMockMvc
class MallPurchaseShipmentApiIntegrationTest {

    private static final AtomicInteger PHONE_SEQUENCE = new AtomicInteger(1);

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("mall_purchase_shipment_test")
            .withUsername("qianxun")
            .withPassword("qianxun-test-password");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private JwtService jwtService;

    private long memberId;
    private long otherMemberId;
    private String memberToken;
    private String otherMemberToken;
    private String adminToken;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @BeforeEach
    void createMembers() {
        memberId = insertMember(0);
        otherMemberId = insertMember(0);
        memberToken = bearer(jwtService.issueMemberToken(memberId));
        otherMemberToken = bearer(jwtService.issueMemberToken(otherMemberId));
        adminToken = bearer(jwtService.issueAdminToken(1L));
    }

    @Test
    void seededImagesUseCosAfterMigration() throws Exception {
        String images = jdbc.queryForObject("SELECT CAST(images AS CHAR) FROM mall_product WHERE name = ?", String.class, "蚕丝被");
        JsonNode list = objectMapper.readTree(images);
        assertThat(list).hasSize(5);
        for (JsonNode image : list) {
            assertThat(image.asText()).startsWith("https://qianxun-1536638067.cos.ap-shanghai.myqcloud.com/");
        }
    }

    @Test
    void memberCanPayForSnapshotAndAdminCanShipAndCompleteIt() throws Exception {
        long productId = createProduct("初始蚕丝被", "199.00");

        mockMvc.perform(get("/api/admin/mall/products").header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.list[0].id").value(productId))
                .andExpect(jsonPath("$.data.list[0].status").value("OFF_SHELF"));
        mockMvc.perform(get("/api/app/mall/products/{id}", productId).header("Authorization", memberToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
        setProductStatus(productId, "ON_SHELF");
        mockMvc.perform(get("/api/app/mall/products").header("Authorization", memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list[0].id").value(productId))
                .andExpect(jsonPath("$.data.list[0].status").value("ON_SHELF"));

        String paymentNo = prepay(productId, memberToken, 0, "初始收货人", phoneFor(memberId), "浙江省湖州市南浔区测试路 1 号");
        mockMvc.perform(get("/api/app/mall/payments/shipping").header("Authorization", memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.shipping_address").doesNotExist());

        updateProduct(productId, "已改名蚕丝被", "299.00");
        setProductStatus(productId, "OFF_SHELF");

        long orderId = mockPay(paymentNo, memberToken);
        String orderNo = jdbc.queryForObject("SELECT order_no FROM mall_order WHERE id = ?", String.class, orderId);
        assertThat(orderNo).matches("QX\\d{8}\\d{4,}");
        mockMvc.perform(post("/api/app/mall/payments/{paymentNo}/mock-pay", paymentNo)
                        .header("Authorization", memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.order_id").value(orderId))
                .andExpect(jsonPath("$.data.order_no").value(orderNo));
        mockMvc.perform(get("/api/app/mall/payments/{paymentNo}", paymentNo).header("Authorization", memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.order_id").value(orderId));
        mockMvc.perform(get("/api/app/mall/payments/shipping").header("Authorization", memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recipient_name").value("初始收货人"))
                .andExpect(jsonPath("$.data.shipping_address").value("浙江省湖州市南浔区测试路 1 号"));

        mockMvc.perform(get("/api/app/mall/orders").header("Authorization", memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].id").value(orderId))
                .andExpect(jsonPath("$.data.list[0].order_no").value(orderNo))
                .andExpect(jsonPath("$.data.list[0].product_name").value("初始蚕丝被"))
                .andExpect(jsonPath("$.data.list[0].product_image").value("https://example.test/mall-product.webp"))
                .andExpect(jsonPath("$.data.list[0].paid_amount").value("199.00"))
                .andExpect(jsonPath("$.data.list[0].status").value("PENDING_SHIPMENT"));
        mockMvc.perform(get("/api/app/mall/orders/{id}", orderId).header("Authorization", memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.order_no").value(orderNo))
                .andExpect(jsonPath("$.data.member_id").doesNotExist())
                .andExpect(jsonPath("$.data.product_name").value("初始蚕丝被"))
                .andExpect(jsonPath("$.data.product_image").value("https://example.test/mall-product.webp"))
                .andExpect(jsonPath("$.data.quantity").value(1))
                .andExpect(jsonPath("$.data.total_amount").value("199.00"))
                .andExpect(jsonPath("$.data.points_used").value(0))
                .andExpect(jsonPath("$.data.paid_amount").value("199.00"))
                .andExpect(jsonPath("$.data.recipient_name").value("初始收货人"))
                .andExpect(jsonPath("$.data.recipient_phone").value(phoneFor(memberId)))
                .andExpect(jsonPath("$.data.shipping_address").value("浙江省湖州市南浔区测试路 1 号"))
                .andExpect(jsonPath("$.data.status").value("PENDING_SHIPMENT"));
        mockMvc.perform(get("/api/admin/mall/orders/{id}", orderId).header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.order_no").value(orderNo))
                .andExpect(jsonPath("$.data.product_name").value("初始蚕丝被"))
                .andExpect(jsonPath("$.data.recipient_phone").value(phoneFor(memberId)));

        mockMvc.perform(post("/api/admin/mall/orders/{id}/complete", orderId).header("Authorization", adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
        mockMvc.perform(post("/api/admin/mall/orders/{id}/ship", orderId).header("Authorization", adminToken)
                        .contentType("application/json").content("{\"express_company\":\"顺丰速运\",\"tracking_no\":\"SF-10001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SHIPPED"))
                .andExpect(jsonPath("$.data.express_company").value("顺丰速运"))
                .andExpect(jsonPath("$.data.tracking_no").value("SF-10001"));
        mockMvc.perform(get("/api/app/mall/orders").header("Authorization", memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list[0].status").value("SHIPPED"))
                .andExpect(jsonPath("$.data.list[0].express_company").value("顺丰速运"))
                .andExpect(jsonPath("$.data.list[0].tracking_no").value("SF-10001"));
        mockMvc.perform(get("/api/app/mall/orders/{id}", orderId).header("Authorization", memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SHIPPED"))
                .andExpect(jsonPath("$.data.express_company").value("顺丰速运"))
                .andExpect(jsonPath("$.data.tracking_no").value("SF-10001"));
        mockMvc.perform(post("/api/admin/mall/orders/{id}/ship", orderId).header("Authorization", adminToken)
                        .contentType("application/json").content("{\"express_company\":\"顺丰速运\",\"tracking_no\":\"SF-10002\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));

        mockMvc.perform(post("/api/admin/mall/orders/{id}/complete", orderId).header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
        mockMvc.perform(post("/api/admin/mall/orders/{id}/complete", orderId).header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        assertThat(jdbc.queryForObject("SELECT points FROM members WHERE id = ?", Integer.class, memberId)).isEqualTo(199);
        assertThat(jdbc.queryForObject("SELECT growth_points FROM members WHERE id = ?", Integer.class, memberId)).isEqualTo(199);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM points_log WHERE mall_order_id = ? AND type = 'ORDER_REWARD'", Integer.class, orderId))
                .isEqualTo(1);
    }

    @Test
    void adminCanFilterMallOrdersByStatusAndKeywordWithPagination() throws Exception {
        String marker = "商城筛选" + Long.toUnsignedString(System.nanoTime());
        long productId = createOnShelfProduct(marker + "商品", "88.00");
        long shippedOrderId = mockPay(prepay(productId, memberToken, 0, marker + "收货人甲", phoneFor(memberId), "筛选地址甲"), memberToken);
        long pendingOrderId = mockPay(prepay(productId, memberToken, 0, marker + "收货人乙", phoneFor(memberId), "筛选地址乙"), memberToken);
        mockPay(prepay(productId, memberToken, 0, marker + "收货人丙", phoneFor(memberId), "筛选地址丙"), memberToken);
        String pendingOrderNo = jdbc.queryForObject(
                "SELECT order_no FROM mall_order WHERE id = ?", String.class, pendingOrderId);

        mockMvc.perform(post("/api/admin/mall/orders/{id}/ship", shippedOrderId).header("Authorization", adminToken)
                        .contentType("application/json").content("{\"express_company\":\"筛选快递\",\"tracking_no\":\"FILTER-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SHIPPED"));

        mockMvc.perform(get("/api/admin/mall/orders").header("Authorization", adminToken)
                        .param("status", "PENDING_SHIPMENT").param("keyword", marker).param("page", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.list.length()").value(2))
                .andExpect(jsonPath("$.data.list[0].status").value("PENDING_SHIPMENT"));

        mockMvc.perform(get("/api/admin/mall/orders").header("Authorization", adminToken)
                        .param("keyword", marker + "商品").param("page", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(3));

        mockMvc.perform(get("/api/admin/mall/orders").header("Authorization", adminToken)
                        .param("keyword", pendingOrderNo).param("page", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].id").value(pendingOrderId));

        mockMvc.perform(get("/api/admin/mall/orders").header("Authorization", adminToken)
                        .param("keyword", marker + "收货人甲").param("page", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].id").value(shippedOrderId));

        mockMvc.perform(get("/api/admin/mall/orders").header("Authorization", adminToken)
                        .param("keyword", phoneFor(memberId)).param("page", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(3));

        mockMvc.perform(get("/api/admin/mall/orders").header("Authorization", adminToken)
                        .param("keyword", marker).param("page", "2").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.data.size").value(1))
                .andExpect(jsonPath("$.data.list.length()").value(1));

    }

    @Test
    void mallEndpointsEnforceRolesAndRejectInvalidProductAndShippingRequests() throws Exception {
        String validProduct = productRequest("鉴权商品", "88.00");
        mockMvc.perform(post("/api/admin/mall/products").contentType("application/json").content(validProduct))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
        mockMvc.perform(post("/api/admin/mall/products").header("Authorization", memberToken)
                        .contentType("application/json").content(validProduct))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
        mockMvc.perform(get("/api/app/mall/products").header("Authorization", adminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
        mockMvc.perform(get("/api/app/mall/products").param("page", "0").header("Authorization", memberToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
        mockMvc.perform(post("/api/admin/mall/products").header("Authorization", adminToken)
                        .contentType("application/json")
                        .content("{\"name\":\" \",\"description\":\"商品说明\",\"images\":[],\"price\":\"88.00\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        long productId = createOnShelfProduct("发货校验商品", "88.00");
        mockMvc.perform(post("/api/app/mall/payments").header("Authorization", memberToken)
                        .contentType("application/json").content("{\"product_id\":" + productId
                                + ",\"quantity\":0,\"points_used\":0,\"recipient_name\":\"收货人\","
                                + "\"recipient_phone\":\"" + phoneFor(memberId) + "\",\"shipping_address\":\"测试地址\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
        mockMvc.perform(post("/api/app/mall/payments").header("Authorization", memberToken)
                        .contentType("application/json").content("{\"product_id\":" + productId
                                + ",\"quantity\":1,\"points_used\":1,\"recipient_name\":\"收货人\","
                                + "\"recipient_phone\":\"" + phoneFor(memberId) + "\",\"shipping_address\":\"测试地址\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(422));
        setProductStatus(productId, "OFF_SHELF");
        mockMvc.perform(post("/api/app/mall/payments").header("Authorization", memberToken)
                        .contentType("application/json").content("{\"product_id\":" + productId
                                + ",\"quantity\":1,\"points_used\":0,\"recipient_name\":\"收货人\","
                                + "\"recipient_phone\":\"" + phoneFor(memberId) + "\",\"shipping_address\":\"测试地址\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
        setProductStatus(productId, "ON_SHELF");
        String paymentNo = prepay(productId, memberToken, 0, "收货人", phoneFor(memberId), "测试地址");
        long orderId = mockPay(paymentNo, memberToken);

        mockMvc.perform(get("/api/app/mall/orders/{id}", orderId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
        mockMvc.perform(get("/api/app/mall/orders/{id}", orderId).header("Authorization", adminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        mockMvc.perform(post("/api/admin/mall/orders/{id}/ship", orderId).header("Authorization", memberToken)
                        .contentType("application/json").content("{\"express_company\":\"快递\",\"tracking_no\":\"NO-1\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
        mockMvc.perform(post("/api/admin/mall/orders/{id}/ship", orderId).header("Authorization", adminToken)
                        .contentType("application/json").content("{\"express_company\":\"\",\"tracking_no\":\"NO-1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
        mockMvc.perform(post("/api/admin/mall/orders/{id}/ship", 999999L).header("Authorization", adminToken)
                        .contentType("application/json").content("{\"express_company\":\"快递\",\"tracking_no\":\"NO-1\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));

        mockMvc.perform(get("/api/admin/mall/orders").header("Authorization", adminToken)
                        .param("status", "NOT_A_STATUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
        mockMvc.perform(get("/api/admin/mall/orders").header("Authorization", adminToken)
                        .param("keyword", "a".repeat(121)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void paymentAndRefundRemainMemberScopedAndRefundedOrderCannotShip() throws Exception {
        jdbc.update("UPDATE members SET points = 300 WHERE id = ?", memberId);
        long productId = createOnShelfProduct("积分退款商品", "100.00");
        String paymentNo = prepay(productId, memberToken, 200, "积分收货人", phoneFor(memberId), "积分退款地址");

        mockMvc.perform(get("/api/app/mall/payments/{paymentNo}", paymentNo).header("Authorization", otherMemberToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
        mockMvc.perform(post("/api/app/mall/payments/{paymentNo}/mock-pay", paymentNo).header("Authorization", otherMemberToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));

        long orderId = mockPay(paymentNo, memberToken);
        mockMvc.perform(get("/api/app/mall/orders").header("Authorization", otherMemberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
        mockMvc.perform(get("/api/app/mall/orders/{id}", orderId).header("Authorization", otherMemberToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
        mockMvc.perform(get("/api/app/mall/orders/{id}", 999999L).header("Authorization", memberToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
        mockMvc.perform(post("/api/app/mall/orders/{id}/refund", orderId).header("Authorization", otherMemberToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
        mockMvc.perform(post("/api/app/mall/orders/{id}/refund", orderId).header("Authorization", memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));
        mockMvc.perform(post("/api/app/mall/orders/{id}/refund", orderId).header("Authorization", memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));
        mockMvc.perform(post("/api/admin/mall/orders/{id}/ship", orderId).header("Authorization", adminToken)
                        .contentType("application/json").content("{\"express_company\":\"快递\",\"tracking_no\":\"NO-2\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));

        assertThat(jdbc.queryForObject("SELECT points FROM members WHERE id = ?", Integer.class, memberId)).isEqualTo(300);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM points_log WHERE mall_order_id = ? AND type = 'REDEEM'", Integer.class, orderId))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM points_log WHERE mall_order_id = ? AND type = 'RETURN'", Integer.class, orderId))
                .isEqualTo(1);
    }

    @Test
    void simultaneousShipmentAndCompletionRequestsPreserveOneStateTransitionAndReward() throws Exception {
        long productId = createOnShelfProduct("并发发货商品", "149.90");
        String paymentNo = prepay(productId, memberToken, 0, "并发收货人", phoneFor(memberId), "并发测试地址");
        long orderId = mockPay(paymentNo, memberToken);

        List<Integer> shipmentStatuses = callConcurrently(() -> mockMvc.perform(
                        post("/api/admin/mall/orders/{id}/ship", orderId).header("Authorization", adminToken)
                                .contentType("application/json")
                                .content("{\"express_company\":\"并发快递\",\"tracking_no\":\"CONCURRENT\"}"))
                .andReturn().getResponse().getStatus());
        assertThat(shipmentStatuses).containsExactlyInAnyOrder(200, 409);
        assertThat(jdbc.queryForObject("SELECT status FROM mall_order WHERE id = ?", String.class, orderId)).isEqualTo("SHIPPED");

        List<Integer> completionStatuses = callConcurrently(() -> mockMvc.perform(
                        post("/api/admin/mall/orders/{id}/complete", orderId).header("Authorization", adminToken))
                .andReturn().getResponse().getStatus());
        assertThat(completionStatuses).containsExactly(200, 200);
        assertThat(jdbc.queryForObject("SELECT status FROM mall_order WHERE id = ?", String.class, orderId)).isEqualTo("COMPLETED");
        assertThat(jdbc.queryForObject("SELECT points FROM members WHERE id = ?", Integer.class, memberId)).isEqualTo(149);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM points_log WHERE mall_order_id = ? AND type = 'ORDER_REWARD'", Integer.class, orderId))
                .isEqualTo(1);
    }

    private long createOnShelfProduct(String name, String price) throws Exception {
        long id = createProduct(name, price);
        setProductStatus(id, "ON_SHELF");
        return id;
    }

    private long createProduct(String name, String price) throws Exception {
        String response = mockMvc.perform(post("/api/admin/mall/products").header("Authorization", adminToken)
                        .contentType("application/json").content(productRequest(name, price)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("OFF_SHELF"))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").asLong();
    }

    private void updateProduct(long productId, String name, String price) throws Exception {
        mockMvc.perform(put("/api/admin/mall/products/{id}", productId).header("Authorization", adminToken)
                        .contentType("application/json").content(productRequest(name, price)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value(name))
                .andExpect(jsonPath("$.data.price").value(price));
    }

    private void setProductStatus(long productId, String statusValue) throws Exception {
        mockMvc.perform(post("/api/admin/mall/products/{id}/status", productId).header("Authorization", adminToken)
                        .contentType("application/json").content("{\"status\":\"" + statusValue + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(statusValue));
    }

    private String prepay(long productId, String token, int pointsUsed, String recipientName, String recipientPhone,
            String address) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "product_id", productId,
                "quantity", 1,
                "points_used", pointsUsed,
                "recipient_name", recipientName,
                "recipient_phone", recipientPhone,
                "shipping_address", address));
        String response = mockMvc.perform(post("/api/app/mall/payments").header("Authorization", token)
                        .contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.payment_method").value("MOCK"))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("payment_no").asText();
    }

    private long mockPay(String paymentNo, String token) throws Exception {
        String response = mockMvc.perform(post("/api/app/mall/payments/{paymentNo}/mock-pay", paymentNo)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("order_id").asLong();
    }

    private String productRequest(String name, String price) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "name", name,
                "description", "可水洗的民宿自营商品",
                "images", List.of("https://example.test/mall-product.webp"),
                "price", new BigDecimal(price)));
    }

    private long insertMember(int points) {
        String phone = String.format("139%08d", PHONE_SEQUENCE.getAndIncrement());
        jdbc.update("INSERT INTO members (phone, points, growth_points) VALUES (?, ?, 0)", phone, points);
        return jdbc.queryForObject("SELECT id FROM members WHERE phone = ?", Long.class, phone);
    }

    private String phoneFor(long id) {
        return jdbc.queryForObject("SELECT phone FROM members WHERE id = ?", String.class, id);
    }

    private List<Integer> callConcurrently(ThrowingStatusCall call) throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> {
                barrier.await();
                return call.call();
            });
            var second = executor.submit(() -> {
                barrier.await();
                return call.call();
            });
            return List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
        }
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    @FunctionalInterface
    private interface ThrowingStatusCall {
        int call() throws Exception;
    }
}
