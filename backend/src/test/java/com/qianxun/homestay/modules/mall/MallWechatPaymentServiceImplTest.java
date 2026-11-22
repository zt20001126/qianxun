package com.qianxun.homestay.modules.mall;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.modules.auth.service.WechatIdentityService;
import com.qianxun.homestay.modules.mall.entity.MallPaymentEntity;
import com.qianxun.homestay.modules.mall.mapper.MallPaymentMapper;
import com.qianxun.homestay.modules.mall.service.impl.MallWechatPaymentServiceImpl;
import com.qianxun.homestay.modules.payment.config.WechatPayProperties;
import com.qianxun.homestay.modules.payment.provider.WechatPayGateway;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayWithRequestPaymentResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

/** 商城微信预支付身份及渠道返回校验。 */
class MallWechatPaymentServiceImplTest {
    @Test
    void createsPrepayForOwnerAndRejectsOtherMember() {
        MallPaymentMapper mapper = mock(MallPaymentMapper.class);
        WechatIdentityService identity = mock(WechatIdentityService.class);
        WechatPayGateway gateway = mock(WechatPayGateway.class);
        WechatPayProperties properties = new WechatPayProperties();
        properties.setEnabled(true);
        MallWechatPaymentServiceImpl service = new MallWechatPaymentServiceImpl(mapper, identity, gateway, properties);
        MallPaymentEntity payment = new MallPaymentEntity();
        payment.setId(8L); payment.setMemberId(4L); payment.setPaymentMethod("WECHAT"); payment.setStatus("PENDING");
        payment.setAmount(new BigDecimal("1.00"));
        payment.setExpiresAt(LocalDateTime.now(ZoneId.of("Asia/Shanghai")).plusMinutes(5));
        when(mapper.findByPaymentNo("M123")).thenReturn(payment);
        assertThatThrownBy(() -> service.createPrepay(5L, "M123", "login", "phone"))
                .isInstanceOf(BusinessException.class);
        verify(identity, never()).verifyAndBindOpenId(5L, "login", "phone");
        when(identity.verifyAndBindOpenId(4L, "login", "phone")).thenReturn("openid");
        when(mapper.claimWechatPrepay(eq(8L), any())).thenReturn(1);
        PrepayWithRequestPaymentResponse response = new PrepayWithRequestPaymentResponse();
        response.setPackageVal("prepay_id=sample");
        when(gateway.createPrepay("M123", payment.getAmount(), "openid", payment.getExpiresAt(), "芊浔民宿商城商品"))
                .thenReturn(response);
        when(mapper.updateWechatPrepayId(8L, "sample")).thenReturn(1);
        assertThat(service.createPrepay(4L, "M123", "login", "phone").packageValue()).isEqualTo("prepay_id=sample");
    }
}
