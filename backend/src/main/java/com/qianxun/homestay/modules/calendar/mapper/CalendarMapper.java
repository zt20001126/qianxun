package com.qianxun.homestay.modules.calendar.mapper;

import com.qianxun.homestay.modules.order.entity.OrderEntity;
import com.qianxun.homestay.modules.room.entity.RoomEntity;
import com.qianxun.homestay.modules.roomblock.entity.RoomBlockEntity;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 查询月历所需的客房、有效订单和封房数据。 */
@Mapper
public interface CalendarMapper {

    /** 按房间楼层和房号顺序读取全部客房。 */
    List<RoomEntity> findRooms();

    /** 读取与指定月份至少占用一个住宿夜晚的非取消订单。 */
    List<OrderEntity> findOverlappingOrders(
            @Param("monthStart") LocalDate monthStart,
            @Param("monthEnd") LocalDate monthEnd,
            @Param("cancelledStatus") String cancelledStatus);

    /** 读取与指定月份至少重叠一天的管理员封房记录。 */
    List<RoomBlockEntity> findOverlappingBlocks(
            @Param("monthStart") LocalDate monthStart,
            @Param("monthEnd") LocalDate monthEnd);
}
