package com.qianxun.homestay.modules.refund.controller;

import com.qianxun.homestay.common.api.ApiResponse;
import com.qianxun.homestay.modules.refund.enums.RefundStatus;
import com.qianxun.homestay.modules.refund.service.RefundService;
import com.qianxun.homestay.modules.refund.vo.RefundPageVO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** P05 后台退款流水查询接口。 */
@RestController
@Validated
@RequestMapping("/api/admin/refunds")
public class AdminRefundController {

    private final RefundService refundService;

    public AdminRefundController(RefundService refundService) {
        this.refundService = refundService;
    }

    /** 按退款状态和北京时间退款日期分页查询。 */
    @GetMapping
    public ApiResponse<RefundPageVO> list(
            @RequestParam(name = "status", required = false) RefundStatus status,
            @RequestParam(name = "start_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate startDate,
            @RequestParam(name = "end_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate endDate,
            @RequestParam(name = "page", defaultValue = "1") @Min(1) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(refundService.findAdminRefunds(status, startDate, endDate, page, size));
    }
}
