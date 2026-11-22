package com.qianxun.homestay.modules.mall.mapper;

import com.qianxun.homestay.modules.mall.entity.MallPaymentEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 商城支付快照持久化接口。 */
@Mapper
public interface MallPaymentMapper {
    /** 保存待支付快照。 */ int insert(MallPaymentEntity payment);
    /** 根据支付单号查询。 */ MallPaymentEntity findByPaymentNo(@Param("paymentNo") String paymentNo);
    /** 按支付单号锁定交易。 */ MallPaymentEntity findByPaymentNoForUpdate(@Param("paymentNo") String paymentNo);
    /** 锁定商城订单对应的支付记录。 */ MallPaymentEntity findByOrderIdForUpdate(@Param("orderId") Long orderId);
    /** 查询商城订单对应的支付记录。 */ MallPaymentEntity findByOrderId(@Param("orderId") Long orderId);
    /** 将待支付交易标记为成功。 */
    int markSuccess(@Param("id") Long id, @Param("orderId") Long orderId,
                    @Param("transactionId") String transactionId, @Param("paidAt") LocalDateTime paidAt);
    /** 关闭到期的待支付交易。 */ int markClosed(@Param("id") Long id);
    /** 保存微信预支付标识。 */
    int updateWechatPrepayId(@Param("id") Long id, @Param("prepayId") String prepayId);
    /** 在到期前标记微信预支付请求正在发起。 */
    int claimWechatPrepay(@Param("id") Long id, @Param("now") LocalDateTime now);
    /** 查询待处理的到期支付编号。 */
    List<Long> findExpiredIds(@Param("now") LocalDateTime now, @Param("limit") int limit);
    /** 根据编号读取支付记录。 */ MallPaymentEntity findById(@Param("id") Long id);
    /** 根据编号锁定支付记录。 */ MallPaymentEntity findByIdForUpdate(@Param("id") Long id);
    /** 将结果未明的交易移至下次对账。 */ int touchPending(@Param("id") Long id);
}
