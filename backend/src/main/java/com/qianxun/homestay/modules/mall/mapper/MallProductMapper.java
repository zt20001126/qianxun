package com.qianxun.homestay.modules.mall.mapper;

import com.qianxun.homestay.modules.mall.entity.MallProductEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 商城商品数据库访问接口，SQL 在 XML 中维护。 */
@Mapper
public interface MallProductMapper {
    /** 分页查询后台商品。 */ List<MallProductEntity> findPage(@Param("offset") long offset, @Param("size") int size);
    /** 分页查询上架商品。 */ List<MallProductEntity> findOnShelfPage(@Param("offset") long offset, @Param("size") int size);
    /** 统计全部商品。 */ long countAll();
    /** 统计上架商品。 */ long countOnShelf();
    MallProductEntity findById(@Param("id") Long id);
    int insert(MallProductEntity product);
    int update(MallProductEntity product);
    int updateStatus(@Param("id") Long id, @Param("status") String status);
}
