package com.qianxun.homestay.modules.mall.service.impl;

import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.modules.mall.entity.MallOrderEntity;
import com.qianxun.homestay.modules.mall.entity.MallPaymentEntity;
import com.qianxun.homestay.modules.mall.mapper.MallOrderMapper;
import com.qianxun.homestay.modules.mall.mapper.MallPaymentMapper;
import com.qianxun.homestay.modules.mall.service.MallRefundFacadeService;
import com.qianxun.homestay.modules.mall.service.MallRefundService;
import com.qianxun.homestay.modules.mall.service.MallWechatRefundService;
import com.qianxun.homestay.modules.mall.vo.MallRefundVO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** 先确认订单归属，再按原支付渠道编排退款。 */
@Service
public class MallRefundFacadeServiceImpl implements MallRefundFacadeService {
    private final MallOrderMapper orders;
    private final MallPaymentMapper payments;
    private final MallRefundService mockRefunds;
    private final MallWechatRefundService wechatRefunds;
    public MallRefundFacadeServiceImpl(MallOrderMapper orders, MallPaymentMapper payments,
            MallRefundService mockRefunds, MallWechatRefundService wechatRefunds) {
        this.orders = orders; this.payments = payments; this.mockRefunds = mockRefunds; this.wechatRefunds = wechatRefunds;
    }
    @Override public MallRefundVO refund(Long memberId, Long orderId) {
        MallOrderEntity order = orders.findById(orderId);
        if (order == null || !memberId.equals(order.getMemberId())) throw new BusinessException(HttpStatus.NOT_FOUND, "订单不存在");
        MallPaymentEntity payment = payments.findByOrderId(orderId);
        if (payment == null) throw new BusinessException(HttpStatus.CONFLICT, "订单没有支付记录");
        if ("WECHAT".equals(payment.getPaymentMethod())) return wechatRefunds.refund(memberId, orderId);
        return mockRefunds.refund(memberId, orderId);
    }
}
