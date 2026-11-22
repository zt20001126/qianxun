package com.qianxun.homestay.modules.order.mapper;

import com.qianxun.homestay.modules.order.dto.OrderQueryRequest;
import com.qianxun.homestay.modules.order.entity.OrderEntity;
import com.qianxun.homestay.modules.order.entity.OrderViewEntity;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 订单表及订单关联查询的数据库访问入口。 */
@Mapper
public interface OrderMapper {

    OrderEntity findById(@Param("id") Long id);

    OrderEntity findByIdForUpdate(@Param("id") Long id);

    /** 按订单 ID 和会员 ID 查询本人订单。 */
    OrderEntity findAppById(@Param("memberId") Long memberId, @Param("id") Long id);

    /** 按订单 ID 和会员 ID 锁定本人订单，串行处理重复取消请求。 */
    OrderEntity findAppByIdForUpdate(@Param("memberId") Long memberId, @Param("id") Long id);

    OrderViewEntity findViewById(@Param("id") Long id);

    List<OrderViewEntity> findPage(@Param("query") OrderQueryRequest query, @Param("offset") long offset);

    /** 查询全部订单，供后台全量导出使用。 */
    List<OrderViewEntity> findAllForExport();

    long count(@Param("query") OrderQueryRequest query);

    /** 查询当前小程序会员的订单页。 */
    List<OrderViewEntity> findAppPage(
            @Param("memberId") Long memberId,
            @Param("status") String status,
            @Param("offset") long offset,
            @Param("size") int size);

    /** 统计当前小程序会员符合筛选条件的订单数。 */
    long countApp(@Param("memberId") Long memberId, @Param("status") String status);

    /** 仅按订单 ID 与会员 ID 查询，避免返回其他会员订单。 */
    OrderViewEntity findAppViewById(
            @Param("memberId") Long memberId,
            @Param("id") Long id);

    /** 查询按 member_id 归属指定会员的订单摘要。 */
    List<OrderViewEntity> findMemberOrders(@Param("memberId") Long memberId);

    /**
     * 查询在指定日期范围内发生入住、续住或退房的有效住宿订单，供每日房态邮件使用。
     *
     * @param startDate 邮件覆盖的首日
     * @param endDate 邮件覆盖的末日（含）
     * @return 含客房、渠道及预订信息的订单列表
     */
    List<OrderViewEntity> findForDailyRoomStatus(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    int insert(OrderEntity order);

    int updateEditableFields(OrderEntity order);

    int updateStatus(@Param("id") Long id, @Param("status") String status);

    /** 将仍处于可取消状态的本人订单标记为已取消。 */
    int cancelForRefund(@Param("id") Long id);

    int attachMemberIfMissing(@Param("id") Long id, @Param("memberId") Long memberId);

    boolean existsPlatformOrderNo(@Param("channelId") Long channelId, @Param("platformOrderNo") String platformOrderNo);

    Long lockOrderNumberGeneration();

    Long findMaxOrderSequence(@Param("orderNoPattern") String orderNoPattern);

    /** 查询商城订单中同一日期已经使用的最大流水号。 */
    Long findMaxMallOrderSequence(@Param("orderNoPattern") String orderNoPattern);

}
