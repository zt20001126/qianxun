package com.qianxun.homestay.modules.payment.service;

import com.qianxun.homestay.modules.payment.vo.MockPayVO;
import com.qianxun.homestay.modules.payment.vo.PaymentStatusVO;

/** 小程序支付状态查询和第一期模拟支付业务接口。 */
public interface MockPaymentService {

    /** 查询当前会员自己的支付交易；超时交易会先按事务规则关闭。 */
    PaymentStatusVO findStatus(Long memberId, String paymentNo);

    /** 模拟支付成功并原子创建正式订单；重复请求返回原订单。 */
    MockPayVO mockPay(Long memberId, String paymentNo);
}
