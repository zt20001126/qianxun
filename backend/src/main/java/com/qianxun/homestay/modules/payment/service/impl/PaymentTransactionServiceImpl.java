package com.qianxun.homestay.modules.payment.service.impl;

import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.modules.availability.service.AvailabilityService;
import com.qianxun.homestay.modules.booking.entity.BookingLockEntity;
import com.qianxun.homestay.modules.booking.enums.BookingLockStatus;
import com.qianxun.homestay.modules.booking.mapper.BookingLockMapper;
import com.qianxun.homestay.modules.member.entity.MemberEntity;
import com.qianxun.homestay.modules.member.mapper.MemberMapper;
import com.qianxun.homestay.modules.order.domain.OrderNoGenerator;
import com.qianxun.homestay.modules.order.entity.OrderAmountEntity;
import com.qianxun.homestay.modules.order.entity.OrderEntity;
import com.qianxun.homestay.modules.order.enums.OrderStatus;
import com.qianxun.homestay.modules.order.mapper.OrderAmountMapper;
import com.qianxun.homestay.modules.order.mapper.OrderMapper;
import com.qianxun.homestay.modules.portrait.service.BookerPortraitService;
import com.qianxun.homestay.modules.payment.domain.PaymentAttemptResult;
import com.qianxun.homestay.modules.payment.entity.PaymentEntity;
import com.qianxun.homestay.modules.payment.enums.PaymentStatus;
import com.qianxun.homestay.modules.payment.enums.PaymentMethod;
import com.qianxun.homestay.modules.payment.mapper.PaymentMapper;
import com.qianxun.homestay.modules.payment.service.PaymentTransactionService;
import com.qianxun.homestay.modules.payment.vo.MockPayVO;
import com.qianxun.homestay.modules.points.service.PointsBalanceService;
import com.qianxun.homestay.modules.room.entity.RoomEntity;
import com.qianxun.homestay.modules.room.mapper.RoomMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

/** 串行处理模拟支付，原子写入正式订单、金额快照和积分抵扣流水。 */
@Service
public class PaymentTransactionServiceImpl implements PaymentTransactionService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final BigDecimal ZERO_AMOUNT = new BigDecimal("0.00");

    private final AvailabilityService availabilityService;
    private final BookingLockMapper bookingLockMapper;
    private final MemberMapper memberMapper;
    private final OrderAmountMapper orderAmountMapper;
    private final OrderMapper orderMapper;
    private final OrderNoGenerator orderNoGenerator;
    private final PaymentMapper paymentMapper;
    private final PointsBalanceService pointsBalanceService;
    private final RoomMapper roomMapper;
    private final BookerPortraitService bookerPortraitService;
    private final Long miniProgramChannelId;
    private final ApplicationEventPublisher eventPublisher;

    public PaymentTransactionServiceImpl(
            AvailabilityService availabilityService,
            BookingLockMapper bookingLockMapper,
            MemberMapper memberMapper,
            OrderAmountMapper orderAmountMapper,
            OrderMapper orderMapper,
            OrderNoGenerator orderNoGenerator,
            PaymentMapper paymentMapper,
            PointsBalanceService pointsBalanceService,
            RoomMapper roomMapper,
            @Value("${app.channel.mini-program-id}") Long miniProgramChannelId,
            BookerPortraitService bookerPortraitService, ApplicationEventPublisher eventPublisher) {
        this.availabilityService = availabilityService;
        this.bookingLockMapper = bookingLockMapper;
        this.memberMapper = memberMapper;
        this.orderAmountMapper = orderAmountMapper;
        this.orderMapper = orderMapper;
        this.orderNoGenerator = orderNoGenerator;
        this.paymentMapper = paymentMapper;
        this.pointsBalanceService = pointsBalanceService;
        this.roomMapper = roomMapper;
        this.bookerPortraitService = bookerPortraitService;
        this.miniProgramChannelId = miniProgramChannelId;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public PaymentAttemptResult pay(Long memberId, String paymentNo) {
        PaymentEntity payment = paymentMapper.findByPaymentNoForUpdate(paymentNo);
        if (payment == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "支付交易不存在");
        }
        BookingLockEntity booking = bookingLockMapper.findByIdForUpdate(payment.getBookingId());
        if (booking == null || !memberId.equals(booking.getMemberId())) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "支付交易不存在");
        }

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return successResult(payment, memberId);
        }
        if (payment.getPaymentMethod() != PaymentMethod.MOCK) {
            throw new BusinessException(HttpStatus.CONFLICT, "该交易不支持模拟支付");
        }
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BusinessException(HttpStatus.CONFLICT, "支付交易已关闭或不可支付");
        }

        LocalDateTime paidAt = now();
        if (!booking.getExpiresAt().isAfter(paidAt)) {
            closeExpired(payment, booking);
            return new PaymentAttemptResult(true, null);
        }
        return settleLocked(payment, booking, memberId, null, paidAt, null);
    }

    @Override
    @Transactional
    public PaymentAttemptResult settleWechatPayment(
            String paymentNo, String transactionId, BigDecimal amount, LocalDateTime paidAt, String payerOpenid) {
        if (transactionId == null || transactionId.isBlank() || payerOpenid == null || payerOpenid.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "微信支付通知缺少交易号或付款人");
        }
        PaymentEntity payment = paymentMapper.findByPaymentNoForUpdate(paymentNo);
        if (payment == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "支付交易不存在");
        }
        BookingLockEntity booking = bookingLockMapper.findByIdForUpdate(payment.getBookingId());
        if (booking == null) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "支付交易关联的临时预订不存在");
        }
        if (payment.getPaymentMethod() != PaymentMethod.WECHAT) {
            throw new BusinessException(HttpStatus.CONFLICT, "支付流水渠道与微信通知不一致");
        }
        if (payment.getAmount().compareTo(amount) != 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "微信通知金额与支付流水不一致");
        }
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            if (!transactionId.equals(payment.getTransactionId())) {
                throw new BusinessException(HttpStatus.CONFLICT, "微信交易号与已结算流水不一致");
            }
            return successResult(payment, booking.getMemberId());
        }
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BusinessException(HttpStatus.CONFLICT, "微信支付流水已关闭或不可支付");
        }
        if (paidAt.isAfter(booking.getExpiresAt())) {
            throw new BusinessException(HttpStatus.CONFLICT, "微信支付时间晚于锁房截止时间");
        }
        return settleLocked(payment, booking, booking.getMemberId(), payerOpenid, paidAt, transactionId);
    }

    private PaymentAttemptResult settleLocked(
            PaymentEntity payment,
            BookingLockEntity booking,
            Long memberId,
            String expectedOpenid,
            LocalDateTime paidAt,
            String transactionId) {
        // 支付通知、模拟支付与超时处理都在支付行锁内串行执行，并沿用库存锁顺序。
        availabilityService.lockRoomsForUpdate(List.of(booking.getRoomId()));
        if (booking.getStatus() != BookingLockStatus.PENDING) {
            if (payment.getPaymentMethod() == PaymentMethod.MOCK
                    && booking.getStatus() == BookingLockStatus.EXPIRED) {
                return new PaymentAttemptResult(true, null);
            }
            throw new BusinessException(HttpStatus.CONFLICT, "临时预订已关闭，无法完成支付");
        }
        if (payment.getPaymentMethod() == PaymentMethod.MOCK && !booking.getExpiresAt().isAfter(paidAt)) {
            closeExpired(payment, booking);
            return new PaymentAttemptResult(true, null);
        }
        if (payment.getAmount().compareTo(booking.getAmount()) != 0) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "支付金额与锁房金额快照不一致");
        }

        RoomEntity room = roomMapper.findEnabledById(booking.getRoomId());
        if (room == null || booking.getGuestCount() > room.getCapacity()) {
            throw new BusinessException(HttpStatus.CONFLICT, "客房资料已变化，请重新预订");
        }
        if (availabilityService.hasConflictExcludingBookingLock(
                booking.getRoomId(), booking.getCheckIn(), booking.getCheckOut(), booking.getId())) {
            throw new BusinessException(HttpStatus.CONFLICT, "所选日期已不可预订，请重新选择");
        }

        MemberEntity member = memberMapper.findByIdForUpdate(memberId);
        if (member == null) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "支付会员不存在");
        }
        if (expectedOpenid != null && !expectedOpenid.equals(member.getOpenid())) {
            throw new BusinessException(HttpStatus.CONFLICT, "微信付款人和会员绑定账号不一致");
        }

        OrderEntity order = createOrder(booking, memberId);
        if (orderMapper.insert(order) != 1) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "创建正式订单失败");
        }
        insertAmountSnapshot(order.getId(), payment.getAmount());
        bookerPortraitService.ensureProfileForOrder(order.getId(), order.getGuestName(), order.getPhone());
        pointsBalanceService.consumeForPaidOrder(
                memberId, booking.getPointsUsed(), booking.getDiscountAmount(), order.getId());

        if (bookingLockMapper.updateStatus(booking.getId(), BookingLockStatus.PAID.name()) != 1) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "更新临时预订状态失败");
        }
        int updated = payment.getPaymentMethod() == PaymentMethod.WECHAT
                ? paymentMapper.markWechatSuccess(payment.getId(), order.getId(), transactionId, paidAt)
                : paymentMapper.markSuccess(payment.getId(), order.getId(), null, paidAt);
        if (updated != 1) {
            throw new BusinessException(HttpStatus.CONFLICT, "支付交易状态已变化，请查询支付结果");
        }
        eventPublisher.publishEvent(new com.qianxun.homestay.modules.notification.event.OrderNotificationEvent(
                com.qianxun.homestay.modules.notification.event.OrderNotificationEvent.Type.STAY_PAID, order.getId(), null, null, paidAt));
        return new PaymentAttemptResult(false, new MockPayVO(
                payment.getPaymentNo(), PaymentStatus.SUCCESS, order.getId(), order.getOrderNo(), money(payment.getAmount())));
    }

    private OrderEntity createOrder(BookingLockEntity booking, Long memberId) {
        OrderEntity order = new OrderEntity();
        order.setOrderNo(orderNoGenerator.nextOrderNo());
        order.setRoomId(booking.getRoomId());
        order.setChannelId(miniProgramChannelId);
        order.setMemberId(memberId);
        order.setGuestName(booking.getGuestName());
        order.setPhone(booking.getPhone());
        order.setGuestCount(booking.getGuestCount());
        order.setCheckIn(booking.getCheckIn());
        order.setCheckOut(booking.getCheckOut());
        order.setStatus(OrderStatus.PENDING_CONFIRM);
        order.setRemark(booking.getRemark());
        return order;
    }

    private void insertAmountSnapshot(Long orderId, BigDecimal paidAmount) {
        OrderAmountEntity amount = new OrderAmountEntity();
        amount.setOrderId(orderId);
        amount.setPaidAmount(paidAmount);
        amount.setPlatformFee(ZERO_AMOUNT);
        amount.setPlatformCommission(ZERO_AMOUNT);
        amount.setExtraCommission(ZERO_AMOUNT);
        amount.setExpectedAmount(null);
        if (orderAmountMapper.insert(amount) != 1) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "写入订单金额快照失败");
        }
    }

    private PaymentAttemptResult successResult(PaymentEntity payment, Long memberId) {
        if (payment.getOrderId() == null) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "成功支付未关联正式订单");
        }
        OrderEntity order = orderMapper.findById(payment.getOrderId());
        if (order == null || !memberId.equals(order.getMemberId())) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "支付关联订单不存在");
        }
        return new PaymentAttemptResult(false, new MockPayVO(
                payment.getPaymentNo(), payment.getStatus(), order.getId(), order.getOrderNo(), money(payment.getAmount())));
    }

    private void closeExpired(PaymentEntity payment, BookingLockEntity booking) {
        MemberEntity member = memberMapper.findByIdForUpdate(booking.getMemberId());
        if (member == null) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "支付会员不存在");
        }
        if (paymentMapper.markClosed(payment.getId()) != 1) {
            throw new BusinessException(HttpStatus.CONFLICT, "支付状态已变化，请查询支付结果");
        }
        if (booking.getStatus() == BookingLockStatus.PENDING
                && bookingLockMapper.updateStatus(booking.getId(), BookingLockStatus.EXPIRED.name()) != 1) {
            throw new BusinessException(HttpStatus.CONFLICT, "临时预订状态已变化，请查询支付结果");
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now(BUSINESS_ZONE);
    }

    private String money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.UNNECESSARY).toPlainString();
    }
}
