package com.qianxun.homestay.modules.mall.service;

import com.qianxun.homestay.modules.mall.vo.MallRefundVO;

/** 商城退款按原支付渠道分发。 */
public interface MallRefundFacadeService {
    /** 对本人商城订单发起原渠道整单退款。 */ MallRefundVO refund(Long memberId, Long orderId);
}
