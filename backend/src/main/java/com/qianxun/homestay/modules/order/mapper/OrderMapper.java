package com.qianxun.homestay.modules.order.mapper;

import com.qianxun.homestay.modules.order.dto.OrderQueryRequest;
import com.qianxun.homestay.modules.order.entity.OrderEntity;
import com.qianxun.homestay.modules.order.entity.OrderViewEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 订单表及订单关联查询的数据库访问入口。 */
@Mapper
public interface OrderMapper {

    OrderEntity findById(@Param("id") Long id);

    OrderEntity findByIdForUpdate(@Param("id") Long id);

    OrderViewEntity findViewById(@Param("id") Long id);

    List<OrderViewEntity> findPage(@Param("query") OrderQueryRequest query, @Param("offset") long offset);

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

    int insert(OrderEntity order);

    int updateEditableFields(OrderEntity order);

    int updateStatus(@Param("id") Long id, @Param("status") String status);

    int attachMemberIfMissing(@Param("id") Long id, @Param("memberId") Long memberId);

    boolean existsPlatformOrderNo(@Param("channelId") Long channelId, @Param("platformOrderNo") String platformOrderNo);

    Long lockOrderNumberGeneration();

    Long findMaxOrderSequence(@Param("orderNoPattern") String orderNoPattern);

}
