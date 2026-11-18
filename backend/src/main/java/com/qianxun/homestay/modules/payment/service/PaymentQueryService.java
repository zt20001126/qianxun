package com.qianxun.homestay.modules.payment.service;

import com.qianxun.homestay.modules.payment.enums.PaymentStatus;
import com.qianxun.homestay.modules.payment.vo.PaymentPageVO;
import java.time.LocalDate;

/** 提供后台模拟支付流水查询。 */
public interface PaymentQueryService {

    /** 按状态和北京时间支付日期分页查询支付流水。 */
    PaymentPageVO findAdminPayments(PaymentStatus status, LocalDate startDate, LocalDate endDate, int page, int size);
}
