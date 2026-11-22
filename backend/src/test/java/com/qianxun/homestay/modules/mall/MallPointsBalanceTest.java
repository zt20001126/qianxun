package com.qianxun.homestay.modules.mall;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.qianxun.homestay.modules.availability.service.AvailabilityService;
import com.qianxun.homestay.modules.booking.mapper.BookingLockMapper;
import com.qianxun.homestay.modules.mall.mapper.MallPaymentReservationMapper;
import com.qianxun.homestay.modules.member.entity.MemberEntity;
import com.qianxun.homestay.modules.member.mapper.MemberMapper;
import com.qianxun.homestay.modules.points.domain.PointsRedemptionCalculator;
import com.qianxun.homestay.modules.points.mapper.PointsLogMapper;
import com.qianxun.homestay.modules.points.service.impl.PointsBalanceServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/** 验证商城和住宿预占共同影响积分可用余额。 */
class MallPointsBalanceTest {
    @Test
    void availableBalanceExcludesBothBusinessesReservations() {
        BookingLockMapper roomMapper = mock(BookingLockMapper.class);
        MallPaymentReservationMapper mallMapper = mock(MallPaymentReservationMapper.class);
        LocalDateTime now = LocalDateTime.of(2026, 11, 22, 10, 0);
        when(roomMapper.sumReservedPoints(1L, now)).thenReturn(70);
        when(mallMapper.sumReservedPoints(1L, now)).thenReturn(40L);
        MemberEntity member = new MemberEntity();
        member.setId(1L);
        member.setPoints(200);
        member.setGrowthPoints(0);
        PointsBalanceServiceImpl service = new PointsBalanceServiceImpl(
                mock(AvailabilityService.class), roomMapper, mallMapper, mock(MemberMapper.class),
                mock(PointsLogMapper.class), new PointsRedemptionCalculator());
        assertThat(service.calculateForLockedMember(member, new BigDecimal("100.00"), 0, now).availablePoints())
                .isEqualTo(90);
    }
}
