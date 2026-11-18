package com.qianxun.homestay.modules.availability.mapper;

import com.qianxun.homestay.modules.availability.entity.AvailabilityRangeEntity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 统一读取各类房间占用，并为写操作锁定客房行。 */
@Mapper
public interface AvailabilityMapper {

    /** 锁定房间记录，串行化同房间的库存写操作。 */
    Long lockRoom(@Param("roomId") Long roomId);

    /** 查询指定区间内与订单、封房或有效锁房重叠的日期段。 */
    List<AvailabilityRangeEntity> findOverlappingRanges(
            @Param("roomId") Long roomId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("now") LocalDateTime now,
            @Param("cancelledOrderStatus") String cancelledOrderStatus,
            @Param("pendingBookingStatus") String pendingBookingStatus,
            @Param("excludedBlockId") Long excludedBlockId,
            @Param("excludedOrderId") Long excludedOrderId,
            @Param("excludedBookingLockId") Long excludedBookingLockId);
}
