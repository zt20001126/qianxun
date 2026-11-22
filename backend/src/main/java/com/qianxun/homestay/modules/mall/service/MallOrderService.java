package com.qianxun.homestay.modules.mall.service;

import com.qianxun.homestay.modules.mall.dto.MallShipRequest;
import com.qianxun.homestay.modules.mall.vo.MallOrderPageVO;
import com.qianxun.homestay.modules.mall.vo.MallOrderVO;

/** 管理后台商城订单业务。 */
public interface MallOrderService {
    /** 分页读取商城订单。 */ MallOrderPageVO findPage(int page, int size);
    /** 分页读取指定会员自己的商城订单。 */ MallOrderPageVO findMemberPage(Long memberId, int page, int size);
    /** 查询商城订单详情。 */ MallOrderVO findById(Long id);
    /** 填写快递信息并发货。 */ MallOrderVO ship(Long id, MallShipRequest request);
    /** 完成已发货订单并发放一次积分奖励。 */ MallOrderVO complete(Long id);
}
