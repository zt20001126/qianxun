package com.qianxun.homestay.modules.mall.controller;

import com.qianxun.homestay.common.api.ApiResponse;
import com.qianxun.homestay.common.security.AuthPrincipal;
import com.qianxun.homestay.modules.mall.dto.MallPrepayRequest;
import com.qianxun.homestay.modules.mall.dto.MallPreviewRequest;
import com.qianxun.homestay.modules.mall.service.MallPaymentService;
import com.qianxun.homestay.modules.mall.service.MallWechatPaymentService;
import com.qianxun.homestay.modules.payment.dto.WechatPrepayRequest;
import com.qianxun.homestay.modules.payment.vo.WechatPrepayVO;
import com.qianxun.homestay.modules.mall.vo.MallPaymentResultVO;
import com.qianxun.homestay.modules.mall.vo.MallPrepayVO;
import com.qianxun.homestay.modules.mall.vo.MallPreviewVO;
import com.qianxun.homestay.modules.mall.vo.MallShippingVO;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 会员商城预支付和支付状态接口。 */
@RestController
@RequestMapping("/api/app/mall/payments")
public class AppMallPaymentController {
    private final MallPaymentService service;
    private final MallWechatPaymentService wechatService;
    public AppMallPaymentController(MallPaymentService service, MallWechatPaymentService wechatService) {
        this.service = service; this.wechatService = wechatService;
    }
    /** 校验会员身份后获取商城微信支付参数。 */
    @PostMapping("/{paymentNo}/wechat-prepay") public ApiResponse<WechatPrepayVO> wechatPrepay(
            @AuthenticationPrincipal AuthPrincipal principal, @PathVariable String paymentNo,
            @Valid @RequestBody WechatPrepayRequest request) {
        return ApiResponse.success(wechatService.createPrepay(principal.id(), paymentNo, request.loginCode(), request.phoneCode()));
    }
    /** 读取会员上次支付成功时保存的收货资料。 */
    @GetMapping("/shipping") public ApiResponse<MallShippingVO> shipping(@AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.success(service.shipping(principal.id()));
    }
    /** 试算积分和应付金额，最终以预支付快照为准。 */
    @PostMapping("/preview") public ApiResponse<MallPreviewVO> preview(@AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody MallPreviewRequest request) {
        return ApiResponse.success(service.preview(principal.id(), request));
    }
    /** 创建待支付快照。 */
    @PostMapping public ApiResponse<MallPrepayVO> prepay(@AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody MallPrepayRequest request) {
        return ApiResponse.success(service.prepay(principal.id(), request));
    }
    /** 查询本人支付状态。 */
    @GetMapping("/{paymentNo}") public ApiResponse<MallPaymentResultVO> status(
            @AuthenticationPrincipal AuthPrincipal principal, @PathVariable String paymentNo) {
        return ApiResponse.success(service.findStatus(principal.id(), paymentNo));
    }
    /** 当前环境采用模拟支付时结算。 */
    @PostMapping("/{paymentNo}/mock-pay") public ApiResponse<MallPaymentResultVO> mockPay(
            @AuthenticationPrincipal AuthPrincipal principal, @PathVariable String paymentNo) {
        return ApiResponse.success(service.mockPay(principal.id(), paymentNo));
    }
}
