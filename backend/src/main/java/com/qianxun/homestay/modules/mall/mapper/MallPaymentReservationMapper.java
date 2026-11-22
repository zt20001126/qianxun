package com.qianxun.homestay.modules.mall.mapper;

import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 查询商城待支付交易占用的会员积分。 */
@Mapper
public interface MallPaymentReservationMapper {
    /** 汇总尚不能释放的商城积分预占。 */
    long sumReservedPoints(@Param("memberId") Long memberId, @Param("now") LocalDateTime now);
}
