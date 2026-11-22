package com.qianxun.homestay.modules.mall.service.impl;

import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.common.util.ApiTime;
import com.qianxun.homestay.modules.mall.dto.MallPrepayRequest;
import com.qianxun.homestay.modules.mall.dto.MallPreviewRequest;
import com.qianxun.homestay.modules.mall.entity.MallOrderEntity;
import com.qianxun.homestay.modules.mall.entity.MallPaymentEntity;
import com.qianxun.homestay.modules.mall.entity.MallProductEntity;
import com.qianxun.homestay.modules.mall.enums.MallProductStatus;
import com.qianxun.homestay.modules.mall.enums.MallOrderStatus;
import com.qianxun.homestay.modules.mall.mapper.MallOrderMapper;
import com.qianxun.homestay.modules.mall.mapper.MallPaymentMapper;
import com.qianxun.homestay.modules.mall.mapper.MallProductMapper;
import com.qianxun.homestay.modules.mall.service.MallPaymentService;
import com.qianxun.homestay.modules.mall.vo.MallPaymentResultVO;
import com.qianxun.homestay.modules.mall.vo.MallPrepayVO;
import com.qianxun.homestay.modules.mall.vo.MallPreviewVO;
import com.qianxun.homestay.modules.mall.vo.MallShippingVO;
import com.qianxun.homestay.modules.member.entity.MemberEntity;
import com.qianxun.homestay.modules.member.mapper.MemberMapper;
import com.qianxun.homestay.modules.order.domain.OrderNoGenerator;
import com.qianxun.homestay.modules.payment.config.WechatPayProperties;
import com.qianxun.homestay.modules.payment.enums.PaymentMethod;
import com.qianxun.homestay.modules.payment.enums.PaymentStatus;
import com.qianxun.homestay.modules.points.domain.PointsRedemptionQuote;
import com.qianxun.homestay.modules.points.entity.PointsLogEntity;
import com.qianxun.homestay.modules.points.enums.PointsLogType;
import com.qianxun.homestay.modules.points.mapper.PointsLogMapper;
import com.qianxun.homestay.modules.points.service.PointsBalanceService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** 商城预支付快照与模拟支付结算。 */
@Service
public class MallPaymentServiceImpl implements MallPaymentService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final int PAYMENT_MINUTES = 10;
    private final MallProductMapper productMapper;
    private final MallPaymentMapper paymentMapper;
    private final MallOrderMapper orderMapper;
    private final MemberMapper memberMapper;
    private final PointsBalanceService pointsService;
    private final PointsLogMapper pointsLogMapper;
    private final WechatPayProperties wechatProperties;
    private final OrderNoGenerator orderNoGenerator;

    public MallPaymentServiceImpl(MallProductMapper productMapper, MallPaymentMapper paymentMapper,
            MallOrderMapper orderMapper, MemberMapper memberMapper, PointsBalanceService pointsService,
            PointsLogMapper pointsLogMapper, WechatPayProperties wechatProperties, OrderNoGenerator orderNoGenerator) {
        this.productMapper = productMapper;
        this.paymentMapper = paymentMapper;
        this.orderMapper = orderMapper;
        this.memberMapper = memberMapper;
        this.pointsService = pointsService;
        this.pointsLogMapper = pointsLogMapper;
        this.wechatProperties = wechatProperties;
        this.orderNoGenerator = orderNoGenerator;
    }

    @Override @Transactional(readOnly = true)
    public MallPreviewVO preview(Long memberId, MallPreviewRequest request) {
        MallProductEntity product = productMapper.findById(request.productId());
        if (product == null || !MallProductStatus.ON_SHELF.name().equals(product.getStatus())) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "商品不存在或已下架");
        }
        MemberEntity member = memberMapper.findById(memberId);
        if (member == null) throw new BusinessException(HttpStatus.NOT_FOUND, "会员不存在");
        BigDecimal total = product.getPrice().multiply(BigDecimal.valueOf(request.quantity()));
        if (total.precision() - total.scale() > 10) throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "商品总额超出允许范围");
        PointsRedemptionQuote quote = pointsService.calculateForLockedMember(member, total, request.pointsUsed(), LocalDateTime.now(BUSINESS_ZONE));
        return new MallPreviewVO(money(quote.roomAmount()), quote.availablePoints(), quote.pointsPerYuan(),
                quote.maxPointsUsable(), quote.pointsUsed(), money(quote.discountAmount()), money(quote.payAmount()));
    }

    @Override public MallShippingVO shipping(Long memberId) {
        MemberEntity member = memberMapper.findByIdWithShipping(memberId);
        if (member == null) throw new BusinessException(HttpStatus.NOT_FOUND, "会员不存在");
        return new MallShippingVO(member.getDefaultRecipientName(), member.getDefaultRecipientPhone(), member.getDefaultShippingAddress());
    }

    @Override @Transactional(isolation = Isolation.READ_COMMITTED)
    public MallPrepayVO prepay(Long memberId, MallPrepayRequest request) {
        MallProductEntity product = productMapper.findById(request.productId());
        if (product == null || !MallProductStatus.ON_SHELF.name().equals(product.getStatus())) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "商品不存在或已下架");
        }
        MemberEntity member = memberMapper.findByIdForUpdate(memberId);
        if (member == null) throw new BusinessException(HttpStatus.NOT_FOUND, "会员不存在");
        BigDecimal total = product.getPrice().multiply(BigDecimal.valueOf(request.quantity()));
        if (total.precision() - total.scale() > 10) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "商品总额超出允许范围");
        }
        LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE);
        PointsRedemptionQuote quote = pointsService.calculateForLockedMember(member, total, request.pointsUsed(), now);
        if (wechatProperties.isEnabled() && quote.payAmount().signum() <= 0) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "微信支付金额必须大于零");
        }
        MallPaymentEntity payment = new MallPaymentEntity();
        // 微信商户单号最长 32 字符，前缀用于与住宿支付回调安全分流。
        payment.setPaymentNo("M" + UUID.randomUUID().toString().replace("-", "").substring(0, 31));
        payment.setMemberId(memberId);
        payment.setProductId(product.getId());
        payment.setProductName(product.getName());
        payment.setQuantity(request.quantity());
        payment.setTotalAmount(quote.roomAmount());
        payment.setPointsUsed(quote.pointsUsed());
        payment.setAmount(quote.payAmount());
        payment.setRecipientName(request.recipientName().trim());
        payment.setRecipientPhone(request.recipientPhone());
        payment.setShippingAddress(request.shippingAddress().trim());
        payment.setPaymentMethod((wechatProperties.isEnabled() ? PaymentMethod.WECHAT : PaymentMethod.MOCK).name());
        payment.setStatus(PaymentStatus.PENDING.name());
        payment.setExpiresAt(now.plusMinutes(PAYMENT_MINUTES));
        if (paymentMapper.insert(payment) != 1) throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "创建商城支付失败");
        return new MallPrepayVO(payment.getPaymentNo(), payment.getPaymentMethod(), money(quote.roomAmount()),
                quote.pointsUsed(), money(quote.discountAmount()), money(quote.payAmount()), ApiTime.fromShanghai(payment.getExpiresAt()));
    }

    @Override @Transactional
    public MallPaymentResultVO mockPay(Long memberId, String paymentNo) {
        if (wechatProperties.isEnabled()) throw new BusinessException(HttpStatus.CONFLICT, "当前环境不支持模拟支付");
        MallPaymentEntity payment = paymentMapper.findByPaymentNoForUpdate(paymentNo);
        requireOwner(payment, memberId);
        if (PaymentStatus.SUCCESS.name().equals(payment.getStatus())) return result(payment);
        if (!PaymentMethod.MOCK.name().equals(payment.getPaymentMethod()) || !PaymentStatus.PENDING.name().equals(payment.getStatus())
                || !payment.getExpiresAt().isAfter(LocalDateTime.now(BUSINESS_ZONE))) {
            throw new BusinessException(HttpStatus.CONFLICT, "交易已关闭或过期");
        }
        return settleLocked(payment, memberId, null, LocalDateTime.now(BUSINESS_ZONE));
    }

    @Override @Transactional
    public MallPaymentResultVO settleWechat(String paymentNo, String transactionId, BigDecimal amount,
            LocalDateTime paidAt, String payerOpenid) {
        if (transactionId == null || transactionId.isBlank() || payerOpenid == null || payerOpenid.isBlank()
                || amount == null || paidAt == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "微信支付通知缺少必要信息");
        }
        MallPaymentEntity payment = paymentMapper.findByPaymentNoForUpdate(paymentNo);
        if (payment == null) throw new BusinessException(HttpStatus.NOT_FOUND, "商城支付交易不存在");
        if (!PaymentMethod.WECHAT.name().equals(payment.getPaymentMethod()) || payment.getAmount().compareTo(amount) != 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "微信支付方式或金额不匹配");
        }
        MemberEntity member = memberMapper.findByIdForUpdate(payment.getMemberId());
        if (member == null || !payerOpenid.equals(member.getOpenid())) {
            throw new BusinessException(HttpStatus.CONFLICT, "微信付款人与会员不匹配");
        }
        if (PaymentStatus.SUCCESS.name().equals(payment.getStatus())) {
            if (!transactionId.equals(payment.getTransactionId())) {
                throw new BusinessException(HttpStatus.CONFLICT, "微信交易号与已结算记录不一致");
            }
            return result(payment);
        }
        if (!PaymentStatus.PENDING.name().equals(payment.getStatus()) || paidAt.isAfter(payment.getExpiresAt())) {
            throw new BusinessException(HttpStatus.CONFLICT, "支付交易已关闭或付款时间超出有效期");
        }
        return settleLocked(payment, member.getId(), transactionId, paidAt);
    }

    private MallPaymentResultVO settleLocked(MallPaymentEntity payment, Long memberId,
            String transactionId, LocalDateTime paidAt) {
        MemberEntity member = memberMapper.findByIdForUpdate(memberId);
        if (member == null || member.getPoints() < payment.getPointsUsed()) {
            throw new BusinessException(HttpStatus.CONFLICT, "会员积分余额不足");
        }
        MallOrderEntity order = new MallOrderEntity();
        order.setOrderNo(orderNoGenerator.nextOrderNo());
        order.setMemberId(memberId);
        order.setProductId(payment.getProductId());
        order.setProductName(payment.getProductName());
        order.setQuantity(payment.getQuantity());
        order.setTotalAmount(payment.getTotalAmount());
        order.setPaidAmount(payment.getAmount());
        order.setPointsUsed(payment.getPointsUsed());
        order.setRecipientName(payment.getRecipientName());
        order.setRecipientPhone(payment.getRecipientPhone());
        order.setShippingAddress(payment.getShippingAddress());
        order.setStatus(MallOrderStatus.PENDING_SHIPMENT.name());
        order.setPaidAt(paidAt);
        if (orderMapper.insert(order) != 1) throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "创建商城订单失败");
        if (payment.getPointsUsed() > 0) consumePoints(member, payment, order.getId());
        if (memberMapper.updateDefaultShipping(memberId, payment.getRecipientName(), payment.getRecipientPhone(),
                payment.getShippingAddress()) != 1 || paymentMapper.markSuccess(payment.getId(), order.getId(), transactionId, paidAt) != 1) {
            throw new BusinessException(HttpStatus.CONFLICT, "支付交易状态已变化");
        }
        return new MallPaymentResultVO(payment.getPaymentNo(), PaymentStatus.SUCCESS.name(), order.getId(), order.getOrderNo(), money(payment.getAmount()));
    }

    @Override public MallPaymentResultVO findStatus(Long memberId, String paymentNo) {
        MallPaymentEntity payment = paymentMapper.findByPaymentNo(paymentNo);
        requireOwner(payment, memberId);
        return result(payment);
    }
    private MallPaymentResultVO result(MallPaymentEntity payment) {
        MallOrderEntity order = payment.getMallOrderId() == null ? null : orderMapper.findById(payment.getMallOrderId());
        if (PaymentStatus.SUCCESS.name().equals(payment.getStatus()) && order == null) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "成功支付未关联商城订单");
        }
        return new MallPaymentResultVO(payment.getPaymentNo(), payment.getStatus(), payment.getMallOrderId(),
                order == null ? null : order.getOrderNo(), money(payment.getAmount()));
    }
    private void consumePoints(MemberEntity member, MallPaymentEntity payment, Long orderId) {
        int balance = member.getPoints() - payment.getPointsUsed();
        if (memberMapper.updatePointsBalance(member.getId(), balance) != 1) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "积分扣减失败");
        }
        PointsLogEntity log = new PointsLogEntity();
        log.setMemberId(member.getId());
        log.setMallOrderId(orderId);
        log.setType(PointsLogType.REDEEM);
        log.setPoints(-payment.getPointsUsed());
        log.setGrowthPoints(0);
        log.setBalanceAfter(balance);
        log.setDiscountAmount(payment.getTotalAmount().subtract(payment.getAmount()));
        log.setRefNo("REDEEM:MALL:" + orderId);
        log.setRemark("商城订单积分抵扣");
        if (pointsLogMapper.insert(log) != 1) throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "积分流水写入失败");
    }
    private void requireOwner(MallPaymentEntity payment, Long memberId) {
        if (payment == null || !memberId.equals(payment.getMemberId())) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "商城支付交易不存在");
        }
    }
    private String money(BigDecimal value) { return value.setScale(2).toPlainString(); }
}
