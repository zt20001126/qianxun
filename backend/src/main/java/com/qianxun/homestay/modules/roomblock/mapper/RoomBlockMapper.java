package com.qianxun.homestay.modules.roomblock.mapper;

import com.qianxun.homestay.modules.roomblock.entity.RoomBlockEntity;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 封房记录的数据访问接口，SQL 维护在 MyBatis XML。 */
@Mapper
public interface RoomBlockMapper {

    /** 按可选房间及月份查询封房记录。 */
    List<RoomBlockEntity> findAll(
            @Param("roomId") Long roomId,
            @Param("monthStart") LocalDate monthStart,
            @Param("monthEnd") LocalDate monthEnd);

    /** 查询单条封房记录。 */
    RoomBlockEntity findById(@Param("id") Long id);

    /** 在房间库存行加锁后锁定封房记录。 */
    RoomBlockEntity findByIdForUpdate(@Param("id") Long id);

    /** 新增封房记录。 */
    int insert(RoomBlockEntity roomBlock);

    /** 更新封房区间和原因。 */
    int update(RoomBlockEntity roomBlock);

    /** 删除整段封房记录。 */
    int delete(@Param("id") Long id);
}
