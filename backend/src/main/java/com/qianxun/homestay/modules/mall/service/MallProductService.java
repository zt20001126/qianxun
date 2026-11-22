package com.qianxun.homestay.modules.mall.service;

import com.qianxun.homestay.modules.mall.dto.MallProductSaveRequest;
import com.qianxun.homestay.modules.mall.enums.MallProductStatus;
import com.qianxun.homestay.modules.mall.vo.MallProductVO;
import com.qianxun.homestay.modules.mall.vo.MallProductPageVO;

/** 商品浏览与后台维护业务。 */
public interface MallProductService {
    /** 分页查询后台商品列表。 */ MallProductPageVO findAdminProducts(int page, int size);
    /** 分页查询已上架商品。 */ MallProductPageVO findOnShelfProducts(int page, int size);
    /** 查询已上架商品详情。 */ MallProductVO getOnShelfProduct(Long id);
    /** 新建默认下架商品。 */ MallProductVO create(MallProductSaveRequest request);
    /** 修改商品资料。 */ MallProductVO update(Long id, MallProductSaveRequest request);
    /** 调整商品上架状态。 */ MallProductVO setStatus(Long id, MallProductStatus status);
}
