package com.qianxun.homestay.modules.payment.mapper;

import com.qianxun.homestay.modules.payment.entity.PaymentEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 模拟支付流水的数据访问接口。 */
@Mapper
public interface PaymentMapper {

    /** 新增待支付流水。 */
    int insert(PaymentEntity payment);

    /** 按支付单号查询支付流水。 */
    PaymentEntity findByPaymentNo(@Param("paymentNo") String paymentNo);

    /** 按支付单号锁定支付流水，支付与超时处理共用此锁。 */
    PaymentEntity findByPaymentNoForUpdate(@Param("paymentNo") String paymentNo);

    /** 按主键锁定支付流水，供超时任务与支付请求串行处理。 */
    PaymentEntity findByIdForUpdate(@Param("id") Long id);

    /** 查询已过期且仍待支付的支付流水编号。 */
    List<Long> findExpiredPaymentIds(@Param("now") LocalDateTime now, @Param("limit") int limit);

    /** 标记支付成功并关联正式订单。 */
    int markSuccess(
            @Param("id") Long id,
            @Param("orderId") Long orderId,
            @Param("paidAt") LocalDateTime paidAt);

    /** 关闭尚未成功的支付流水。 */
    int markClosed(@Param("id") Long id);
}
