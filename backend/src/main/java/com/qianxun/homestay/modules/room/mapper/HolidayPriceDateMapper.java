package com.qianxun.homestay.modules.room.mapper;

import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 节假日计价日期的持久化接口。 */
@Mapper
public interface HolidayPriceDateMapper {

    /** 查询闭区间内启用节假日价的日期。 */
    List<LocalDate> findDates(@Param("firstDate") LocalDate firstDate, @Param("lastDate") LocalDate lastDate);

    /** 保存某天的启用记录，已存在时保持原记录。 */
    void enable(@Param("date") LocalDate date);

    /** 删除某天的启用记录。 */
    void disable(@Param("date") LocalDate date);
}
