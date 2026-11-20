package com.qianxun.homestay.modules.refund.mapper;

import com.qianxun.homestay.modules.refund.entity.RefundEntity;
import com.qianxun.homestay.modules.refund.enums.RefundStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 退款流水数据库访问入口，SQL 统一维护在 RefundMapper.xml。 */
@Mapper
public interface RefundMapper {

    /** 写入一笔退款流水。 */
    int insert(RefundEntity refund);

    /** 查询订单已有的成功退款，供重复取消请求返回原结果。 */
    RefundEntity findSuccessfulByOrderId(@Param("orderId") Long orderId);

    /** 按商户退款单号锁定微信退款流水。 */
    RefundEntity findByRefundNoForUpdate(@Param("refundNo") String refundNo);

    /** 查询订单最近一笔退款流水。 */
    RefundEntity findLatestByOrderId(@Param("orderId") Long orderId);

    /** 记录微信侧成功退款及微信退款单号。 */
    int markWechatSuccess(
            @Param("id") Long id, @Param("wechatRefundId") String wechatRefundId,
            @Param("refundedAt") LocalDateTime refundedAt);

    /** 记录微信侧明确失败的退款结果。 */
    int markFailed(@Param("id") Long id, @Param("reason") String reason);

    /** 分页查询后台退款流水。 */
    List<RefundEntity> findAdminPage(
            @Param("status") RefundStatus status,
            @Param("startAt") LocalDateTime startAt,
            @Param("endBefore") LocalDateTime endBefore,
            @Param("offset") long offset,
            @Param("size") int size);

    /** 统计后台退款流水筛选结果。 */
    long countAdmin(
            @Param("status") RefundStatus status,
            @Param("startAt") LocalDateTime startAt,
            @Param("endBefore") LocalDateTime endBefore);
}
