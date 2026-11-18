package com.qianxun.homestay.modules.channel.mapper;

import com.qianxun.homestay.modules.channel.entity.ChannelEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 固定渠道持久化接口。 */
@Mapper
public interface ChannelMapper {

    List<ChannelEntity> findAll();

    ChannelEntity findById(@Param("id") Long id);

    int updateEnabled(@Param("id") Long id, @Param("enabled") boolean enabled);
}
