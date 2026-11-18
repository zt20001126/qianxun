package com.qianxun.homestay.modules.points.mapper;

import com.qianxun.homestay.modules.points.entity.PointsLogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/** 积分流水数据库访问入口。 */
@Mapper
public interface PointsLogMapper {

    boolean existsByRefNo(@Param("refNo") String refNo);

    /** 按稳定业务号读取积分流水，用于退款返积分和幂等校验。 */
    PointsLogEntity findByRefNo(@Param("refNo") String refNo);

    int insert(PointsLogEntity pointsLog);

    /** 按会员、订单、流水类型筛选积分流水并分页。 */
    List<PointsLogEntity> findPage(
            @Param("memberId") Long memberId, @Param("orderId") Long orderId,
            @Param("type") String type, @Param("offset") long offset, @Param("size") int size);

    /** 统计积分流水筛选结果数量。 */
    long countPage(@Param("memberId") Long memberId, @Param("orderId") Long orderId,
            @Param("type") String type);
}
