package com.qianxun.homestay.modules.mall;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.modules.mall.dto.MallProductSaveRequest;
import com.qianxun.homestay.modules.mall.entity.MallProductEntity;
import com.qianxun.homestay.modules.mall.mapper.MallProductMapper;
import com.qianxun.homestay.modules.mall.service.impl.MallProductServiceImpl;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 覆盖商城商品新建状态及下架商品访问边界。 */
class MallProductServiceImplTest {
    private final MallProductMapper mapper = mock(MallProductMapper.class);
    private final com.qianxun.homestay.common.cache.DisplayCacheService cache = mock(com.qianxun.homestay.common.cache.DisplayCacheService.class);
    private final MallProductServiceImpl service = new MallProductServiceImpl(mapper, cache, new com.qianxun.homestay.config.DisplayCacheProperties());

    @org.junit.jupiter.api.BeforeEach
    void setUpCache() {
        org.mockito.Mockito.lenient().when(cache.get(any(), any(), any(), any(), any()))
                .thenAnswer(invocation -> ((java.util.function.Supplier<?>) invocation.getArgument(4)).get());
    }

    @Test
    void newlyCreatedProductsAreNotAutomaticallyPublished() {
        MallProductEntity[] inserted = new MallProductEntity[1];
        doAnswer(invocation -> {
            inserted[0] = invocation.getArgument(0);
            inserted[0].setId(7L);
            return 1;
        }).when(mapper).insert(any(MallProductEntity.class));
        when(mapper.findById(7L)).thenAnswer(invocation -> inserted[0]);

        var result = service.create(new MallProductSaveRequest("蚕丝被", "蚕丝被", List.of(), new BigDecimal("1.00")));

        assertThat(result.status()).isEqualTo("OFF_SHELF");
        assertThat(result.price()).isEqualTo("1.00");
    }

    @Test
    void customerCannotViewAnUnpublishedProduct() {
        MallProductEntity product = new MallProductEntity();
        product.setStatus("OFF_SHELF");
        when(mapper.findById(7L)).thenReturn(product);
        assertThatThrownBy(() -> service.getOnShelfProduct(7L)).isInstanceOf(BusinessException.class);
    }
}
