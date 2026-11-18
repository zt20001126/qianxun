package com.qianxun.homestay.modules.payment.controller;

import com.qianxun.homestay.common.api.ApiResponse;
import com.qianxun.homestay.modules.payment.enums.PaymentStatus;
import com.qianxun.homestay.modules.payment.service.PaymentQueryService;
import com.qianxun.homestay.modules.payment.vo.PaymentPageVO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** P04 后台支付流水查询接口。 */
@RestController
@Validated
@RequestMapping("/api/admin/payments")
public class AdminPaymentController {

    private final PaymentQueryService paymentQueryService;

    public AdminPaymentController(PaymentQueryService paymentQueryService) {
        this.paymentQueryService = paymentQueryService;
    }

    /** 按支付状态和北京时间支付日期分页查询。 */
    @GetMapping
    public ApiResponse<PaymentPageVO> list(
            @RequestParam(name = "status", required = false) PaymentStatus status,
            @RequestParam(name = "start_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate startDate,
            @RequestParam(name = "end_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate endDate,
            @RequestParam(name = "page", defaultValue = "1") @Min(1) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(paymentQueryService.findAdminPayments(status, startDate, endDate, page, size));
    }
}
