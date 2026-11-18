package com.qianxun.homestay.modules.creator.mapper;

import com.qianxun.homestay.modules.creator.entity.CreatorEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 达人资料持久化接口。 */
@Mapper
public interface CreatorMapper {

    List<CreatorEntity> findAll(
            @Param("channelId") Long channelId,
            @Param("enabled") Boolean enabled,
            @Param("name") String name);

    CreatorEntity findById(@Param("id") Long id);

    int insert(CreatorEntity creator);

    int update(CreatorEntity creator);

    int updateEnabled(@Param("id") Long id, @Param("enabled") boolean enabled);
}
