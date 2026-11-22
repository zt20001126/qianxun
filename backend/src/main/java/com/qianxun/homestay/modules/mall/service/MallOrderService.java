package com.qianxun.homestay.modules.mall.service;

import com.qianxun.homestay.modules.mall.dto.MallOrderQueryRequest;
import com.qianxun.homestay.modules.mall.dto.MallShipRequest;
import com.qianxun.homestay.modules.mall.vo.AppMallOrderDetailVO;
import com.qianxun.homestay.modules.mall.vo.MallOrderPageVO;
import com.qianxun.homestay.modules.mall.vo.MallOrderVO;

/** 商城订单查询、履约与完成业务。 */
public interface MallOrderService {
    /** 按筛选条件分页读取商城订单。 */ MallOrderPageVO findPage(MallOrderQueryRequest request);
    /** 分页读取指定会员自己的商城订单。 */ MallOrderPageVO findMemberPage(Long memberId, int page, int size);
    /** 查询指定会员自己的商城订单详情。 */ AppMallOrderDetailVO findMemberById(Long memberId, Long id);
    /** 查询商城订单详情。 */ MallOrderVO findById(Long id);
    /** 填写快递信息并发货。 */ MallOrderVO ship(Long id, MallShipRequest request);
    /** 完成已发货订单并发放一次积分奖励。 */ MallOrderVO complete(Long id);
}
