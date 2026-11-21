package com.qianxun.homestay.modules.payment.controller;

import com.qianxun.homestay.common.api.ApiResponse;
import com.qianxun.homestay.common.security.AuthPrincipal;
import com.qianxun.homestay.modules.payment.dto.WechatPrepayRequest;
import com.qianxun.homestay.modules.payment.service.WechatPaymentService;
import com.qianxun.homestay.modules.payment.vo.WechatPrepayVO;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 当前会员的微信小程序预支付接口。 */
@RestController
@RequestMapping("/api/app/payments")
public class AppWechatPaymentController {

    private final WechatPaymentService wechatPaymentService;

    public AppWechatPaymentController(WechatPaymentService wechatPaymentService) {
        this.wechatPaymentService = wechatPaymentService;
    }

    /** 验证当前微信身份与会员绑定后创建微信预支付。 */
    @PostMapping("/{paymentNo}/wechat-prepay")
    public ApiResponse<WechatPrepayVO> prepay(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable String paymentNo,
            @Valid @RequestBody WechatPrepayRequest request) {
        return ApiResponse.success(wechatPaymentService.createPrepay(
                principal.id(), paymentNo, request.loginCode()));
    }
}
