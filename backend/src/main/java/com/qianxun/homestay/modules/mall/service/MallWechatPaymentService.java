package com.qianxun.homestay.modules.mall.service;

import com.qianxun.homestay.modules.payment.vo.WechatPrepayVO;

/** 商城微信预支付能力。 */
public interface MallWechatPaymentService {
    /** 核验会员身份后创建微信预支付。 */
    WechatPrepayVO createPrepay(Long memberId, String paymentNo, String loginCode, String phoneCode);
}
