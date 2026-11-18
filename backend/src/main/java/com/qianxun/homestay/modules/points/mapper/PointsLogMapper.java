package com.qianxun.homestay.modules.points.mapper;

import com.qianxun.homestay.modules.points.entity.PointsLogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 积分流水数据库访问入口。 */
@Mapper
public interface PointsLogMapper {

    boolean existsByRefNo(@Param("refNo") String refNo);

    int insert(PointsLogEntity pointsLog);
}
