package com.qianxun.homestay.modules.refund.controller;

import com.qianxun.homestay.common.api.ApiResponse;
import com.qianxun.homestay.common.security.AuthPrincipal;
import com.qianxun.homestay.modules.refund.dto.RefundRequest;
import com.qianxun.homestay.modules.refund.service.RefundService;
import com.qianxun.homestay.modules.refund.vo.RefundResultVO;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** P03 小程序本人订单的自助取消与同步模拟退款接口。 */
@RestController
@RequestMapping("/api/app/orders")
public class AppRefundController {

    private final RefundService refundService;

    public AppRefundController(RefundService refundService) {
        this.refundService = refundService;
    }

    /** 校验取消资格后同步模拟退款；请求体和取消原因均可省略。 */
    @PostMapping("/{id}/refunds")
    public ApiResponse<RefundResultVO> refund(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody(required = false) RefundRequest request) {
        return ApiResponse.success(refundService.refundMyOrder(
                principal.id(), id, request == null ? null : request.reason()));
    }
}
