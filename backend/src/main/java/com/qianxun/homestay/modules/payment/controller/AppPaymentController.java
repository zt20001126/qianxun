package com.qianxun.homestay.modules.payment.controller;

import com.qianxun.homestay.common.api.ApiResponse;
import com.qianxun.homestay.common.security.AuthPrincipal;
import com.qianxun.homestay.modules.payment.service.MockPaymentService;
import com.qianxun.homestay.modules.payment.vo.MockPayVO;
import com.qianxun.homestay.modules.payment.vo.PaymentStatusVO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** P02/P08 小程序模拟支付状态查询及模拟支付接口。 */
@RestController
@RequestMapping("/api/app/payments")
public class AppPaymentController {

    private final MockPaymentService mockPaymentService;

    public AppPaymentController(MockPaymentService mockPaymentService) {
        this.mockPaymentService = mockPaymentService;
    }

    /** 查询当前会员支付交易状态。 */
    @GetMapping("/{paymentNo}")
    public ApiResponse<PaymentStatusVO> status(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable String paymentNo) {
        return ApiResponse.success(mockPaymentService.findStatus(principal.id(), paymentNo));
    }

    /** 明确点击支付后，由服务端以预支付快照金额模拟成功。 */
    @PostMapping("/{paymentNo}/mock-pay")
    public ApiResponse<MockPayVO> mockPay(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable String paymentNo) {
        return ApiResponse.success(mockPaymentService.mockPay(principal.id(), paymentNo));
    }
}
