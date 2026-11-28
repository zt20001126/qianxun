package com.qianxun.homestay.modules.booking.service.impl;

import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.common.util.ApiTime;
import com.qianxun.homestay.modules.availability.domain.StayDateRange;
import com.qianxun.homestay.modules.availability.service.AvailabilityService;
import com.qianxun.homestay.modules.booking.dto.PrepayRequest;
import com.qianxun.homestay.modules.booking.entity.BookingLockEntity;
import com.qianxun.homestay.modules.booking.enums.BookingLockStatus;
import com.qianxun.homestay.modules.booking.mapper.BookingLockMapper;
import com.qianxun.homestay.modules.booking.service.BookingLockService;
import com.qianxun.homestay.modules.member.entity.MemberEntity;
import com.qianxun.homestay.modules.member.mapper.MemberMapper;
import com.qianxun.homestay.modules.payment.entity.PaymentEntity;
import com.qianxun.homestay.modules.payment.enums.PaymentStatus;
import com.qianxun.homestay.modules.payment.enums.PaymentMethod;
import com.qianxun.homestay.modules.payment.config.WechatPayProperties;
import com.qianxun.homestay.modules.payment.mapper.PaymentMapper;
import com.qianxun.homestay.modules.payment.vo.PrepayVO;
import com.qianxun.homestay.modules.points.domain.PointsRedemptionQuote;
import com.qianxun.homestay.modules.points.service.PointsBalanceService;
import com.qianxun.homestay.modules.room.entity.RoomEntity;
import com.qianxun.homestay.modules.room.mapper.RoomMapper;
import com.qianxun.homestay.modules.room.service.RoomPriceService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

/** 创建带价格快照的临时预订，并串行占用客房与会员积分额度。 */
@Service
public class BookingLockServiceImpl implements BookingLockService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final int LOCK_DURATION_MINUTES = 10;

    private final AvailabilityService availabilityService;
    private final BookingLockMapper bookingLockMapper;
    private final MemberMapper memberMapper;
    private final PaymentMapper paymentMapper;
    private final PointsBalanceService pointsBalanceService;
    private final RoomMapper roomMapper;
    private final RoomPriceService roomPriceService;
    private final WechatPayProperties wechatPayProperties;

    public BookingLockServiceImpl(
            AvailabilityService availabilityService,
            BookingLockMapper bookingLockMapper,
            MemberMapper memberMapper,
            PaymentMapper paymentMapper,
            PointsBalanceService pointsBalanceService,
            RoomMapper roomMapper,
            RoomPriceService roomPriceService,
            WechatPayProperties wechatPayProperties) {
        this.availabilityService = availabilityService;
        this.bookingLockMapper = bookingLockMapper;
        this.memberMapper = memberMapper;
        this.paymentMapper = paymentMapper;
        this.pointsBalanceService = pointsBalanceService;
        this.roomMapper = roomMapper;
        this.roomPriceService = roomPriceService;
        this.wechatPayProperties = wechatPayProperties;
    }

    @Override
    // 等待会员锁后必须读取最新已提交的商城与住宿预占，避免可重复读旧快照导致积分超占。
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public PrepayVO prepay(Long memberId, PrepayRequest request) {
        availabilityService.validateMiniProgramRange(request.checkIn(), request.checkOut());

        // 先锁客房再锁会员，使同房库存和同会员积分预占都按数据库行锁串行化。
        availabilityService.lockRoomsForUpdate(List.of(request.roomId()));
        RoomEntity room = roomMapper.findEnabledById(request.roomId());
        if (room == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "房间不存在或已下架");
        }
        if (request.guestCount() > room.getCapacity()) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "入住人数超过客房容量");
        }
        MemberEntity member = memberMapper.findByIdForUpdate(memberId);
        if (member == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "会员不存在");
        }

        LocalDateTime createdAt = now();
        if (!availabilityService.isAvailable(request.roomId(), request.checkIn(), request.checkOut())) {
            throw new BusinessException(HttpStatus.CONFLICT, "所选日期已不可预订，请重新选择");
        }
        // 在锁房事务内重新报价，支付阶段使用本次写入的金额快照。
        BigDecimal roomAmount = roomPriceService.quote(room, new StayDateRange(request.checkIn(), request.checkOut()));
        PointsRedemptionQuote quote = pointsBalanceService.calculateForLockedMember(
                member, roomAmount, request.pointsUsed(), createdAt);

        BookingLockEntity bookingLock = new BookingLockEntity();
        bookingLock.setMemberId(memberId);
        bookingLock.setRoomId(request.roomId());
        bookingLock.setCheckIn(request.checkIn());
        bookingLock.setCheckOut(request.checkOut());
        bookingLock.setGuestName(request.guestName());
        bookingLock.setPhone(request.phone());
        bookingLock.setGuestCount(request.guestCount());
        bookingLock.setRemark(request.remark());
        bookingLock.setRoomAmount(quote.roomAmount());
        bookingLock.setDiscountAmount(quote.discountAmount());
        bookingLock.setPointsUsed(quote.pointsUsed());
        bookingLock.setAmount(quote.payAmount());
        bookingLock.setStatus(BookingLockStatus.PENDING);
        bookingLock.setExpiresAt(createdAt.plusMinutes(LOCK_DURATION_MINUTES));
        if (bookingLockMapper.insert(bookingLock) != 1) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "创建临时预订失败");
        }

        PaymentEntity payment = new PaymentEntity();
        payment.setBookingId(bookingLock.getId());
        payment.setPaymentNo(UUID.randomUUID().toString().replace("-", ""));
        payment.setPaymentMethod(wechatPayProperties.isEnabled() ? PaymentMethod.WECHAT : PaymentMethod.MOCK);
        payment.setAmount(quote.payAmount());
        payment.setStatus(PaymentStatus.PENDING);
        if (paymentMapper.insert(payment) != 1) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "创建支付交易失败");
        }
        return new PrepayVO(
                bookingLock.getId(), payment.getPaymentNo(), payment.getPaymentMethod(), money(quote.roomAmount()),
                quote.pointsUsed(), money(quote.discountAmount()), money(quote.payAmount()),
                ApiTime.fromShanghai(bookingLock.getExpiresAt()));
    }

    private LocalDateTime now() {
        LocalDateTime current = LocalDateTime.now(BUSINESS_ZONE);
        return current.withNano(current.getNano() / 1_000_000 * 1_000_000);
    }

    private String money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.UNNECESSARY).toPlainString();
    }
}
