package com.qianxun.homestay.modules.mall.service;

import com.qianxun.homestay.modules.mall.vo.MallRefundVO;

/** 商城订单退款业务。 */
public interface MallRefundService {
    /** 为本人待发货订单发起整单退款。 */ MallRefundVO refund(Long memberId, Long orderId);
}
