package com.qianxun.homestay.modules.mall.service;

import com.qianxun.homestay.modules.mall.dto.MallPrepayRequest;
import com.qianxun.homestay.modules.mall.dto.MallPreviewRequest;
import com.qianxun.homestay.modules.mall.vo.MallPaymentResultVO;
import com.qianxun.homestay.modules.mall.vo.MallPrepayVO;
import com.qianxun.homestay.modules.mall.vo.MallPreviewVO;
import com.qianxun.homestay.modules.mall.vo.MallShippingVO;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 商城支付交易业务。 */
public interface MallPaymentService {
    /** 创建待支付价格快照并预占积分。 */ MallPrepayVO prepay(Long memberId, MallPrepayRequest request);
    /** 试算商品总额和积分抵扣。 */ MallPreviewVO preview(Long memberId, MallPreviewRequest request);
    /** 查询本人最近付款成功时的收货信息。 */ MallShippingVO shipping(Long memberId);
    /** 以模拟方式结算指定会员的交易。 */ MallPaymentResultVO mockPay(Long memberId, String paymentNo);
    /** 查询指定会员的交易状态。 */ MallPaymentResultVO findStatus(Long memberId, String paymentNo);
    /** 验证渠道结果后幂等结算商城微信支付。 */
    MallPaymentResultVO settleWechat(String paymentNo, String transactionId, BigDecimal amount,
            LocalDateTime paidAt, String payerOpenid);
}
