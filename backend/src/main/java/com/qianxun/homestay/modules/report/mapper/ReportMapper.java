package com.qianxun.homestay.modules.report.mapper;

import com.qianxun.homestay.modules.report.entity.DashboardRoomEntity;
import com.qianxun.homestay.modules.report.entity.ReportBlockEntity;
import com.qianxun.homestay.modules.report.entity.ReportOrderEntity;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 经营总览及月报使用的数据访问接口。 */
@Mapper
public interface ReportMapper {

    /** 读取与指定月份至少占用一个住宿夜晚的非取消订单及金额快照。 */
    List<ReportOrderEntity> findMonthOrders(
            @Param("monthStart") LocalDate monthStart,
            @Param("monthEnd") LocalDate monthEnd,
            @Param("cancelledStatus") String cancelledStatus);

    /** 读取与指定月份相交的管理员封房日期区间。 */
    List<ReportBlockEntity> findMonthBlocks(
            @Param("monthStart") LocalDate monthStart, @Param("monthEnd") LocalDate monthEnd);

    /** 读取指定日期的房态卡片及其订单和封房信息。 */
    List<DashboardRoomEntity> findDashboardRooms(
            @Param("date") LocalDate date, @Param("cancelledStatus") String cancelledStatus);
}
