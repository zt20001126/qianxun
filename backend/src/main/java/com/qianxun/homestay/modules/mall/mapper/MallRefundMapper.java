package com.qianxun.homestay.modules.mall.mapper;

import com.qianxun.homestay.modules.mall.entity.MallRefundEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 商城退款记录数据库访问。 */
@Mapper
public interface MallRefundMapper {
    /** 查找已成功退款的单号。 */ String findSuccessfulNo(@Param("orderId") Long orderId);
    /** 写入模拟退款成功记录。 */
    int insertSuccess(@Param("orderId") Long orderId, @Param("paymentId") Long paymentId,
            @Param("refundNo") String refundNo, @Param("amount") BigDecimal amount,
            @Param("refundedAt") LocalDateTime refundedAt);
    /** 查询订单最近一笔退款流水。 */ MallRefundEntity findLatest(@Param("orderId") Long orderId);
    /** 根据退款单号查询流水。 */ MallRefundEntity findByNo(@Param("refundNo") String refundNo);
    /** 锁定指定退款流水。 */ MallRefundEntity findByNoForUpdate(@Param("refundNo") String refundNo);
    /** 创建待处理微信退款。 */ int insertPending(MallRefundEntity refund);
    /** 渠道确认成功后更新流水。 */
    int markSuccess(@Param("id") Long id, @Param("wechatRefundId") String wechatRefundId,
            @Param("refundedAt") LocalDateTime refundedAt);
    /** 渠道明确失败时更新流水。 */ int markFailed(@Param("id") Long id);
}
