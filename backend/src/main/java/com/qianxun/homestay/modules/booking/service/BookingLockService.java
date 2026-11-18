package com.qianxun.homestay.modules.booking.service;

import com.qianxun.homestay.modules.booking.dto.PrepayRequest;
import com.qianxun.homestay.modules.payment.vo.PrepayVO;

/** 小程序预支付与临时锁房业务接口。 */
public interface BookingLockService {

    /** 校验日期、房态和积分后创建十分钟待支付交易。 */
    PrepayVO prepay(Long memberId, PrepayRequest request);
}
