package com.qianxun.homestay.modules.room.mapper;

import com.qianxun.homestay.modules.room.entity.RoomEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 客房资料持久化接口，SQL 统一维护在 mapper XML。 */
@Mapper
public interface RoomMapper {

    List<RoomEntity> findAll();

    List<RoomEntity> findEnabled();

    RoomEntity findById(@Param("id") Long id);

    RoomEntity findEnabledById(@Param("id") Long id);

    boolean existsNumber(@Param("number") String number);

    boolean existsNumberForOtherRoom(@Param("number") String number, @Param("id") Long id);

    int insert(RoomEntity room);

    int update(RoomEntity room);

    int updateEnabled(@Param("id") Long id, @Param("enabled") boolean enabled);
}
