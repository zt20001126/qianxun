package com.qianxun.homestay.modules.mall;

import static org.assertj.core.api.Assertions.assertThat;
import com.qianxun.homestay.modules.mall.dto.MallProductSaveRequest;
import com.qianxun.homestay.modules.mall.enums.MallProductStatus;
import com.qianxun.homestay.modules.mall.service.MallProductService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** 验证商品事务提交后真实 Redis 中详情和分页同步失效。 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = "app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
class MallProductCacheIntegrationTest {
    @Container static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0");
    @Container static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7.4.7-alpine").withExposedPorts(6379);
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }
    @Autowired MallProductService products;
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    com.qianxun.homestay.modules.mall.mapper.MallProductMapper mapper;
    @Test void updatesAndUnpublishesCachedProduct() {
        var created = products.create(new MallProductSaveRequest("商品", "介绍", List.of("https://example.com/a.jpg"), new BigDecimal("10.00")));
        products.setStatus(created.id(), MallProductStatus.ON_SHELF);
        org.mockito.Mockito.clearInvocations(mapper);
        assertThat(products.getOnShelfProduct(created.id()).price()).isEqualTo("10.00");
        assertThat(products.getOnShelfProduct(created.id()).images()).containsExactly("https://example.com/a.jpg");
        org.mockito.Mockito.verify(mapper, org.mockito.Mockito.times(1)).findById(created.id());
        products.findOnShelfProducts(1, 20);
        products.update(created.id(), new MallProductSaveRequest("商品", "介绍", List.of("https://example.com/b.jpg"), new BigDecimal("20.00")));
        assertThat(products.getOnShelfProduct(created.id()).price()).isEqualTo("20.00");
        assertThat(products.findOnShelfProducts(1, 20).list()).anyMatch(item -> item.id().equals(created.id()) && item.images().contains("https://example.com/b.jpg"));
        products.setStatus(created.id(), MallProductStatus.OFF_SHELF);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> products.getOnShelfProduct(created.id())).isInstanceOf(com.qianxun.homestay.common.exception.BusinessException.class);
        assertThat(products.findOnShelfProducts(1, 20).list()).noneMatch(item -> item.id().equals(created.id()));
    }
}
