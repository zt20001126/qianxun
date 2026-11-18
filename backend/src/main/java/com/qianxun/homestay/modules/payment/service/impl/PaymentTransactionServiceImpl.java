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
import com.qianxun.homestay.modules.payment.domain.PaymentAttemptResult;
import com.qianxun.homestay.modules.payment.entity.PaymentEntity;
import com.qianxun.homestay.modules.payment.enums.PaymentStatus;
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
    private final Long miniProgramChannelId;

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
            @Value("${app.channel.mini-program-id}") Long miniProgramChannelId) {
        this.availabilityService = availabilityService;
        this.bookingLockMapper = bookingLockMapper;
        this.memberMapper = memberMapper;
        this.orderAmountMapper = orderAmountMapper;
        this.orderMapper = orderMapper;
        this.orderNoGenerator = orderNoGenerator;
        this.paymentMapper = paymentMapper;
        this.pointsBalanceService = pointsBalanceService;
        this.roomMapper = roomMapper;
        this.miniProgramChannelId = miniProgramChannelId;
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
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BusinessException(HttpStatus.CONFLICT, "支付交易已关闭或不可支付");
        }

        // 支付与超时任务使用相同的支付、锁房、客房、会员锁顺序，避免互相覆盖交易结果。
        availabilityService.lockRoomsForUpdate(List.of(booking.getRoomId()));
        LocalDateTime paidAt = now();
        if (booking.getStatus() != BookingLockStatus.PENDING || !booking.getExpiresAt().isAfter(paidAt)) {
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

        OrderEntity order = createOrder(booking, memberId);
        if (orderMapper.insert(order) != 1) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "创建正式订单失败");
        }
        insertAmountSnapshot(order.getId(), payment.getAmount());
        pointsBalanceService.consumeForPaidOrder(
                memberId, booking.getPointsUsed(), booking.getDiscountAmount(), order.getId());

        if (bookingLockMapper.updateStatus(booking.getId(), BookingLockStatus.PAID.name()) != 1) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "更新临时预订状态失败");
        }
        if (paymentMapper.markSuccess(payment.getId(), order.getId(), paidAt) != 1) {
            throw new BusinessException(HttpStatus.CONFLICT, "支付交易状态已变化，请查询支付结果");
        }
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
