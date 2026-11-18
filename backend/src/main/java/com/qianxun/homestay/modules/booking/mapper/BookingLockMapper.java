package com.qianxun.homestay.modules.booking.mapper;

import com.qianxun.homestay.modules.booking.entity.BookingLockEntity;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 临时预订锁房记录的数据访问接口。 */
@Mapper
public interface BookingLockMapper {

    /** 新增一条待支付锁房记录。 */
    int insert(BookingLockEntity bookingLock);

    /** 按主键查询临时预订。 */
    BookingLockEntity findById(@Param("id") Long id);

    /** 按主键锁定临时预订，支付与超时处理共用此锁。 */
    BookingLockEntity findByIdForUpdate(@Param("id") Long id);

    /** 统计会员尚未过期的待支付积分预占总数。 */
    int sumReservedPoints(@Param("memberId") Long memberId, @Param("now") LocalDateTime now);

    /** 更新临时预订状态。 */
    int updateStatus(@Param("id") Long id, @Param("status") String status);
}
