package com.qianxun.homestay.modules.refund.service;

import com.qianxun.homestay.modules.refund.enums.RefundStatus;
import com.qianxun.homestay.modules.refund.vo.RefundPageVO;
import com.qianxun.homestay.modules.refund.vo.RefundResultVO;
import java.time.LocalDate;

/** 负责小程序自助模拟退款及后台退款流水查询。 */
public interface RefundService {

    /** 校验本人订单并在同一事务内退款、取消订单和返还抵扣积分。 */
    RefundResultVO refundMyOrder(Long memberId, Long orderId, String reason);

    /** 查询本人订单最近一笔退款流水状态。 */
    RefundResultVO findMyLatestRefund(Long memberId, Long orderId);

    /** 按状态和北京时间退款日期分页查询后台退款流水。 */
    RefundPageVO findAdminRefunds(RefundStatus status, LocalDate startDate, LocalDate endDate, int page, int size);
}
