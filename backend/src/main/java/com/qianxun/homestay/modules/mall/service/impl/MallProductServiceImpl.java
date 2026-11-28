package com.qianxun.homestay.modules.mall.service.impl;

import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.common.cache.DisplayCacheService;
import com.qianxun.homestay.config.DisplayCacheProperties;
import com.qianxun.homestay.common.util.ApiTime;
import com.qianxun.homestay.modules.mall.dto.MallProductSaveRequest;
import com.qianxun.homestay.modules.mall.entity.MallProductEntity;
import com.qianxun.homestay.modules.mall.enums.MallProductStatus;
import com.qianxun.homestay.modules.mall.mapper.MallProductMapper;
import com.qianxun.homestay.modules.mall.service.MallProductService;
import com.qianxun.homestay.modules.mall.vo.MallProductVO;
import com.qianxun.homestay.modules.mall.vo.MallProductPageVO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 实现商城商品展示及管理员维护。 */
@Service
public class MallProductServiceImpl implements MallProductService {
    private static final String CACHE_DOMAIN = "mall";
    private final DisplayCacheProperties cacheProperties;
    private final MallProductMapper mapper;
    private final DisplayCacheService cache;

    public MallProductServiceImpl(MallProductMapper mapper, DisplayCacheService cache, DisplayCacheProperties cacheProperties) { this.mapper = mapper; this.cache = cache; this.cacheProperties = cacheProperties; }

    @Override public MallProductPageVO findAdminProducts(int page, int size) {
        return new MallProductPageVO(mapper.findPage((long) (page - 1) * size, size).stream().map(this::toVO).toList(),
                mapper.countAll(), page, size);
    }
    @Override public MallProductPageVO findOnShelfProducts(int page, int size) {
        if (size != cacheProperties.getPageSize() || page > cacheProperties.getMaxPages()) return loadOnShelfPage(page, size);
        return cache.get(CACHE_DOMAIN, "page:" + page + ":" + size, cacheProperties.getPageTtl(), MallProductPageVO.class,
                () -> loadOnShelfPage(page, size));
    }
    private MallProductPageVO loadOnShelfPage(int page, int size) {
        return new MallProductPageVO(mapper.findOnShelfPage((long) (page - 1) * size, size).stream().map(this::toVO).toList(),
                mapper.countOnShelf(), page, size);
    }
    @Override public MallProductVO getOnShelfProduct(Long id) {
        return cache.get(CACHE_DOMAIN, "product:" + id, cacheProperties.getProductTtl(), MallProductVO.class, () -> loadOnShelfProduct(id));
    }
    private MallProductVO loadOnShelfProduct(Long id) {
        MallProductEntity product = requireProduct(id);
        if (!MallProductStatus.ON_SHELF.name().equals(product.getStatus())) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "商品不存在");
        }
        return toVO(product);
    }
    @Override @Transactional public MallProductVO create(MallProductSaveRequest request) {
        MallProductEntity product = toEntity(request);
        product.setStatus(MallProductStatus.OFF_SHELF.name());
        mapper.insert(product);
        cache.invalidateAfterCommit(CACHE_DOMAIN);
        return toVO(requireProduct(product.getId()));
    }
    @Override @Transactional public MallProductVO update(Long id, MallProductSaveRequest request) {
        requireProduct(id);
        MallProductEntity product = toEntity(request);
        product.setId(id);
        mapper.update(product);
        cache.invalidateAfterCommit(CACHE_DOMAIN);
        return toVO(requireProduct(id));
    }
    @Override @Transactional public MallProductVO setStatus(Long id, MallProductStatus status) {
        requireProduct(id);
        mapper.updateStatus(id, status.name());
        cache.invalidateAfterCommit(CACHE_DOMAIN);
        return toVO(requireProduct(id));
    }
    private MallProductEntity requireProduct(Long id) {
        MallProductEntity product = mapper.findById(id);
        if (product == null) throw new BusinessException(HttpStatus.NOT_FOUND, "商品不存在");
        return product;
    }
    private MallProductEntity toEntity(MallProductSaveRequest request) {
        MallProductEntity product = new MallProductEntity();
        product.setName(request.name().trim());
        product.setDescription(request.description().trim());
        product.setImages(request.images());
        product.setPrice(request.price());
        return product;
    }
    private MallProductVO toVO(MallProductEntity product) {
        return new MallProductVO(product.getId(), product.getName(), product.getDescription(), product.getImages(),
                product.getPrice().setScale(2).toPlainString(), product.getStatus(),
                ApiTime.fromShanghai(product.getCreatedAt()), ApiTime.fromShanghai(product.getUpdatedAt()));
    }
}
