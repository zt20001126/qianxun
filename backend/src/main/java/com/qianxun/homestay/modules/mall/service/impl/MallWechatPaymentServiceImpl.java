package com.qianxun.homestay.modules.mall.service.impl;

import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.modules.auth.service.WechatIdentityService;
import com.qianxun.homestay.modules.mall.entity.MallPaymentEntity;
import com.qianxun.homestay.modules.mall.mapper.MallPaymentMapper;
import com.qianxun.homestay.modules.mall.service.MallWechatPaymentService;
import com.qianxun.homestay.modules.payment.config.WechatPayProperties;
import com.qianxun.homestay.modules.payment.enums.PaymentMethod;
import com.qianxun.homestay.modules.payment.enums.PaymentStatus;
import com.qianxun.homestay.modules.payment.provider.WechatPayGateway;
import com.qianxun.homestay.modules.payment.vo.WechatPrepayVO;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayWithRequestPaymentResponse;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** 复用会员身份绑定与微信支付网关创建商城预支付。 */
@Service
public class MallWechatPaymentServiceImpl implements MallWechatPaymentService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final String PREPAY_PREFIX = "prepay_id=";
    private final MallPaymentMapper payments;
    private final WechatIdentityService identity;
    private final WechatPayGateway gateway;
    private final WechatPayProperties properties;

    public MallWechatPaymentServiceImpl(MallPaymentMapper payments, WechatIdentityService identity,
            WechatPayGateway gateway, WechatPayProperties properties) {
        this.payments = payments; this.identity = identity; this.gateway = gateway; this.properties = properties;
    }
    @Override public WechatPrepayVO createPrepay(Long memberId, String paymentNo, String loginCode, String phoneCode) {
        if (!properties.isEnabled()) throw new BusinessException(HttpStatus.CONFLICT, "微信支付当前未启用");
        MallPaymentEntity payment = payments.findByPaymentNo(paymentNo);
        if (payment == null || !memberId.equals(payment.getMemberId()) || !PaymentMethod.WECHAT.name().equals(payment.getPaymentMethod())) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "商城微信支付交易不存在");
        }
        if (!PaymentStatus.PENDING.name().equals(payment.getStatus()) || !payment.getExpiresAt().isAfter(LocalDateTime.now(BUSINESS_ZONE))) {
            throw new BusinessException(HttpStatus.CONFLICT, "支付交易已关闭或过期");
        }
        String openid = identity.verifyAndBindOpenId(memberId, loginCode, phoneCode);
        // 先记录外部请求在途；超时扫描看到该标记时必须保留预占，直到渠道结果明确。
        if (payments.claimWechatPrepay(payment.getId(), LocalDateTime.now(BUSINESS_ZONE)) != 1) {
            throw new BusinessException(HttpStatus.CONFLICT, "微信预支付正在处理或交易已过期，请稍后查询");
        }
        PrepayWithRequestPaymentResponse response = gateway.createPrepay(
                paymentNo, payment.getAmount(), openid, payment.getExpiresAt(), "芊浔民宿商城商品");
        String value = response == null ? null : response.getPackageVal();
        if (value == null || !value.startsWith(PREPAY_PREFIX) || value.length() <= PREPAY_PREFIX.length()) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "微信预支付响应无效");
        }
        if (payments.updateWechatPrepayId(payment.getId(), value.substring(PREPAY_PREFIX.length())) != 1) {
            throw new BusinessException(HttpStatus.CONFLICT, "支付交易状态已变化，请查询支付结果");
        }
        return new WechatPrepayVO(response.getAppId(), response.getTimeStamp(), response.getNonceStr(),
                value, response.getSignType(), response.getPaySign());
    }
}
