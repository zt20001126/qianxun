package com.qianxun.homestay.modules.mall.mapper;

import com.qianxun.homestay.modules.mall.dto.MallOrderQueryRequest;
import com.qianxun.homestay.modules.mall.entity.MallOrderEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 商城订单数据库访问，SQL 位于对应 XML。 */
@Mapper
public interface MallOrderMapper {
    /** 写入支付成功后的商城正式订单。 */
    int insert(MallOrderEntity order);
    /** 管理员按筛选条件分页读取订单。 */
    List<MallOrderEntity> findPage(
            @Param("query") MallOrderQueryRequest query,
            @Param("offset") long offset);
    /** 按会员分页读取自己的商城订单。 */
    List<MallOrderEntity> findMemberPage(@Param("memberId") Long memberId, @Param("offset") long offset, @Param("size") int size);
    /** 统计会员自己的商城订单。 */
    long countMember(@Param("memberId") Long memberId);
    /** 统计符合管理员筛选条件的商城订单数量。 */
    long count(@Param("query") MallOrderQueryRequest query);
    /** 按主键读取订单。 */
    MallOrderEntity findById(@Param("id") Long id);
    /** 按订单和会员归属读取订单，避免会员跨订单访问。 */
    MallOrderEntity findMemberById(@Param("memberId") Long memberId, @Param("id") Long id);
    /** 锁定订单以串行化状态变化。 */
    MallOrderEntity findByIdForUpdate(@Param("id") Long id);
    /** 待发货订单填写快递信息并发货。 */
    int markShipped(@Param("id") Long id, @Param("company") String company, @Param("trackingNo") String trackingNo);
    /** 已发货订单完成。 */
    int markCompleted(@Param("id") Long id);
    /** 将待发货订单标记为已退款。 */ int markRefunded(@Param("id") Long id);
    /** 判断订单是否有尚未确认结果的退款。 */
    boolean hasPendingRefund(@Param("id") Long id);
}
