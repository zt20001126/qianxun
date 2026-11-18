package com.qianxun.homestay.modules.booking.controller;

import com.qianxun.homestay.common.api.ApiResponse;
import com.qianxun.homestay.common.security.AuthPrincipal;
import com.qianxun.homestay.modules.booking.dto.PrepayRequest;
import com.qianxun.homestay.modules.booking.service.BookingLockService;
import com.qianxun.homestay.modules.payment.vo.PrepayVO;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** P01 小程序预支付及十分钟临时锁房接口。 */
@RestController
@RequestMapping("/api/app/payments")
public class AppBookingController {

    private final BookingLockService bookingLockService;

    public AppBookingController(BookingLockService bookingLockService) {
        this.bookingLockService = bookingLockService;
    }

    /** 创建待支付交易并返回后端计算的费用快照。 */
    @PostMapping("/prepay")
    public ApiResponse<PrepayVO> prepay(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody PrepayRequest request) {
        return ApiResponse.success(bookingLockService.prepay(principal.id(), request));
    }
}
