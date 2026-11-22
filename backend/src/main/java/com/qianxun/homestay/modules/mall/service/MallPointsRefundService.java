package com.qianxun.homestay.modules.mall.service;

import com.qianxun.homestay.modules.mall.entity.MallOrderEntity;
import com.qianxun.homestay.modules.member.entity.MemberEntity;

/** 商城退款时的积分返还业务。 */
public interface MallPointsRefundService {
    /** 在持有会员行锁的现有事务中返还订单抵扣积分。 */
    void returnPoints(MemberEntity member, MallOrderEntity order);
}
