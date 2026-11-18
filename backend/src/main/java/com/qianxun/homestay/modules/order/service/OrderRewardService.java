package com.qianxun.homestay.modules.order.service;

/** 订单模块依赖的积分奖励扩展点，具体会员与流水处理由积分模块提供。 */
public interface OrderRewardService {

    /** 为符合条件的已完成订单发放一次奖励；无有效会员手机号时不执行发放。 */
    void rewardIfEligible(Long orderId);
}
