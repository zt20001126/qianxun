package com.qianxun.homestay.modules.payment.service.impl;

import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.modules.availability.service.AvailabilityService;
import com.qianxun.homestay.modules.booking.entity.BookingLockEntity;
import com.qianxun.homestay.modules.booking.enums.BookingLockStatus;
import com.qianxun.homestay.modules.booking.mapper.BookingLockMapper;
import com.qianxun.homestay.modules.member.mapper.MemberMapper;
import com.qianxun.homestay.modules.payment.entity.PaymentEntity;
import com.qianxun.homestay.modules.payment.enums.PaymentStatus;
import com.qianxun.homestay.modules.payment.mapper.PaymentMapper;
import com.qianxun.homestay.modules.payment.service.PaymentTimeoutService;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 在独立事务内关闭过期交易并释放临时锁房及积分预占。 */
@Service
public class PaymentTimeoutServiceImpl implements PaymentTimeoutService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final AvailabilityService availabilityService;
    private final BookingLockMapper bookingLockMapper;
    private final MemberMapper memberMapper;
    private final PaymentMapper paymentMapper;

    public PaymentTimeoutServiceImpl(
            AvailabilityService availabilityService,
            BookingLockMapper bookingLockMapper,
            MemberMapper memberMapper,
            PaymentMapper paymentMapper) {
        this.availabilityService = availabilityService;
        this.bookingLockMapper = bookingLockMapper;
        this.memberMapper = memberMapper;
        this.paymentMapper = paymentMapper;
    }

    @Override
    @Transactional
    public void closeExpiredPayment(Long paymentId) {
        PaymentEntity payment = paymentMapper.findByIdForUpdate(paymentId);
        if (payment == null || payment.getStatus() != PaymentStatus.PENDING) {
            return;
        }
        BookingLockEntity booking = bookingLockMapper.findByIdForUpdate(payment.getBookingId());
        if (booking == null) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "支付交易关联的临时预订不存在");
        }
        if (booking.getStatus() != BookingLockStatus.PENDING) {
            // 已成功或已关闭的锁房不能被超时扫描再次释放。
            if (booking.getStatus() == BookingLockStatus.PAID) {
                throw new BusinessException(HttpStatus.CONFLICT, "支付与锁房状态不一致");
            }
            paymentMapper.markClosed(payment.getId());
            return;
        }
        if (booking.getExpiresAt().isAfter(now())) {
            return;
        }

        availabilityService.lockRoomsForUpdate(List.of(booking.getRoomId()));
        if (memberMapper.findByIdForUpdate(booking.getMemberId()) == null) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "支付交易关联会员不存在");
        }

        // P08 和超时扫描先争用 payment 行锁，只有先拿到锁且检查未过期的一方能提交状态转换。
        if (paymentMapper.markClosed(payment.getId()) != 1) {
            return;
        }
        if (bookingLockMapper.updateStatus(booking.getId(), BookingLockStatus.EXPIRED.name()) != 1) {
            throw new BusinessException(HttpStatus.CONFLICT, "临时预订状态已变化，请重试");
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now(BUSINESS_ZONE);
    }
}
