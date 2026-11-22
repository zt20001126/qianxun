package com.qianxun.homestay.modules.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qianxun.homestay.modules.notification.config.MailNotificationProperties;
import com.qianxun.homestay.modules.notification.service.impl.DailyRoomStatusNotificationServiceImpl;
import com.qianxun.homestay.modules.notification.vo.DailyRoomStatusDayVO;
import com.qianxun.homestay.modules.notification.vo.DailyRoomStatusRowVO;
import com.qianxun.homestay.modules.order.entity.OrderViewEntity;
import com.qianxun.homestay.modules.order.enums.OrderStatus;
import com.qianxun.homestay.modules.order.mapper.OrderMapper;
import com.qianxun.homestay.modules.room.entity.RoomEntity;
import com.qianxun.homestay.modules.room.mapper.RoomMapper;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDate;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/** 验证每日房态邮件按三日逐房展示入住、续住、退房及空房。 */
class DailyRoomStatusNotificationServiceImplTest {

    private final MailNotificationProperties properties = new MailNotificationProperties();
    private final JavaMailSender sender = mock(JavaMailSender.class);
    private final SpringTemplateEngine templates = mock(SpringTemplateEngine.class);
    private final RoomMapper roomMapper = mock(RoomMapper.class);
    private final OrderMapper orderMapper = mock(OrderMapper.class);
    private final DailyRoomStatusNotificationServiceImpl service = new DailyRoomStatusNotificationServiceImpl(
            properties, sender, templates, roomMapper, orderMapper);

    @Test
    void sendsThreeDaysWithCheckInContinuationCheckoutAndVacantRoom() {
        LocalDate firstDate = LocalDate.of(2026, 11, 22);
        properties.setRecipients("manager@example.com");
        RoomEntity room101 = room(101L, "101", "临河大床房");
        RoomEntity room102 = room(102L, "102", "庭院双床房");
        OrderViewEntity order101 = order(101L, "101", firstDate, firstDate.plusDays(2), "张三", "13800000000");
        OrderViewEntity cancelled = order(102L, "102", firstDate, firstDate.plusDays(1), "李四", "13900000000");
        cancelled.setStatus(OrderStatus.CANCELLED);
        when(roomMapper.findAll()).thenReturn(List.of(room101, room102));
        when(orderMapper.findForDailyRoomStatus(firstDate, firstDate.plusDays(2))).thenReturn(List.of(order101));
        when(templates.process(eq("mail/daily-room-status"), any(Context.class))).thenReturn("<html>report</html>");
        when(sender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));

        service.sendDailyRoomStatus(firstDate);

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templates).process(eq("mail/daily-room-status"), contextCaptor.capture());
        @SuppressWarnings("unchecked")
        List<DailyRoomStatusDayVO> days = (List<DailyRoomStatusDayVO>) contextCaptor.getValue().getVariable("days");
        assertThat(days).hasSize(3);
        assertThat(days.get(0).getRows()).extracting(DailyRoomStatusRowVO::getRoomStatus)
                .containsExactly("入住", "空房");
        assertThat(days.get(1).getRows()).extracting(DailyRoomStatusRowVO::getRoomStatus)
                .containsExactly("续住", "空房");
        assertThat(days.get(2).getRows()).extracting(DailyRoomStatusRowVO::getRoomStatus)
                .containsExactly("退房", "空房");
        DailyRoomStatusRowVO checkInRow = days.get(0).getRows().getFirst();
        assertThat(checkInRow.getOrderStatus()).isEqualTo("待入住（PENDING_CHECKIN）");
        assertThat(checkInRow.getGuestName()).isEqualTo("张三");
        assertThat(checkInRow.getPhone()).isEqualTo("13800000000");
        assertThat(checkInRow.getTotalNights()).isEqualTo(2);
        verify(sender).send(any(MimeMessage.class));
        verify(orderMapper).findForDailyRoomStatus(firstDate, firstDate.plusDays(2));
    }

    @Test
    void doesNotQueryOrSendWhenRecipientsAreNotConfigured() {
        service.sendDailyRoomStatus(LocalDate.of(2026, 11, 22));

        verify(roomMapper, never()).findAll();
        verify(orderMapper, never()).findForDailyRoomStatus(any(), any());
        verify(sender, never()).send(any(MimeMessage.class));
    }

    @Test
    void templateRendersDetailedRoomStatusRows() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        SpringTemplateEngine actualTemplates = new SpringTemplateEngine();
        actualTemplates.setTemplateResolver(resolver);
        DailyRoomStatusRowVO row = new DailyRoomStatusRowVO("101", "临河大床房", "入住",
                "待入住（PENDING_CHECKIN）", "QX202611220001", "小程序", "张三", "13800000000", 2,
                "2026-11-22", "2026-11-24", 2, "需要婴儿床", "提前联系");
        Context context = new Context();
        context.setVariable("generatedAt", "2026-11-22");
        context.setVariable("days", List.of(new DailyRoomStatusDayVO("2026-11-22", "今日", List.of(row))));

        String html = actualTemplates.process("mail/daily-room-status", context);

        assertThat(html).contains("今日 · 2026-11-22", "张三 / 13800000000", "需要婴儿床", "提前联系");
    }

    private RoomEntity room(Long id, String number, String name) {
        RoomEntity room = new RoomEntity();
        room.setId(id);
        room.setNumber(number);
        room.setName(name);
        return room;
    }

    private OrderViewEntity order(Long roomId, String roomNumber, LocalDate checkIn, LocalDate checkOut,
            String guestName, String phone) {
        OrderViewEntity order = new OrderViewEntity();
        order.setRoomId(roomId);
        order.setRoomNumber(roomNumber);
        order.setOrderNo("QX202611220001");
        order.setChannelName("小程序");
        order.setGuestName(guestName);
        order.setPhone(phone);
        order.setGuestCount(2);
        order.setCheckIn(checkIn);
        order.setCheckOut(checkOut);
        order.setNights(2);
        order.setStatus(OrderStatus.PENDING_CHECKIN);
        order.setRemark("需要婴儿床");
        order.setAdminRemark("提前联系");
        return order;
    }
}
