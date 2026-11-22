package com.qianxun.homestay.modules.notification.service.impl;

import com.qianxun.homestay.modules.notification.config.MailNotificationProperties;
import com.qianxun.homestay.modules.notification.service.DailyRoomStatusNotificationService;
import com.qianxun.homestay.modules.notification.vo.DailyRoomStatusDayVO;
import com.qianxun.homestay.modules.notification.vo.DailyRoomStatusRowVO;
import com.qianxun.homestay.modules.order.entity.OrderViewEntity;
import com.qianxun.homestay.modules.order.enums.OrderStatus;
import com.qianxun.homestay.modules.order.mapper.OrderMapper;
import com.qianxun.homestay.modules.room.entity.RoomEntity;
import com.qianxun.homestay.modules.room.mapper.RoomMapper;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

/** 汇总三日有效订单并渲染为管理员易读的房态邮件。 */
@Service
public class DailyRoomStatusNotificationServiceImpl implements DailyRoomStatusNotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DailyRoomStatusNotificationServiceImpl.class);
    private static final int REPORT_DAY_COUNT = 3;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final List<String> DAY_NAMES = List.of("今日", "明日", "后天");

    private final MailNotificationProperties properties;
    private final JavaMailSender sender;
    private final SpringTemplateEngine templates;
    private final RoomMapper roomMapper;
    private final OrderMapper orderMapper;

    public DailyRoomStatusNotificationServiceImpl(MailNotificationProperties properties, JavaMailSender sender,
            SpringTemplateEngine templates, RoomMapper roomMapper, OrderMapper orderMapper) {
        this.properties = properties;
        this.sender = sender;
        this.templates = templates;
        this.roomMapper = roomMapper;
        this.orderMapper = orderMapper;
    }

    /** {@inheritDoc} */
    @Override
    public void sendDailyRoomStatus(LocalDate firstDate) {
        if (properties.recipientList().isEmpty()) {
            LOGGER.warn("每日房态邮件未配置收件人，firstDate={}", firstDate);
            return;
        }

        try {
            // Step 1：一次性查询三日内入住、续住或退房的所有有效订单，避免逐房逐日查询。
            LocalDate lastDate = firstDate.plusDays(REPORT_DAY_COUNT - 1L);
            List<OrderViewEntity> orders = orderMapper.findForDailyRoomStatus(firstDate, lastDate);
            List<DailyRoomStatusDayVO> days = buildDays(firstDate, roomMapper.findAll(), orders);

            // Step 2：渲染后向既有管理员收件人配置逐一发送。
            Context context = new Context();
            context.setVariable("days", days);
            context.setVariable("generatedAt", DATE_FORMATTER.format(firstDate));
            String html = templates.process("mail/daily-room-status", context);
            String subject = "[芊浔民宿] 三日房态安排（" + DATE_FORMATTER.format(firstDate) + "起）";
            for (String recipient : properties.recipientList()) {
                var message = sender.createMimeMessage();
                var helper = new MimeMessageHelper(message, "UTF-8");
                if (!properties.getFromEmail().isBlank()) {
                    helper.setFrom(properties.getFromEmail(), properties.getSenderName());
                }
                helper.setTo(recipient);
                helper.setSubject(subject);
                helper.setText(html, true);
                sender.send(message);
            }
        } catch (Exception exception) {
            // 定时通知失败不能影响应用主业务，按既有邮件通知策略记录错误即可。
            LOGGER.error("每日房态邮件发送失败，firstDate={}", firstDate, exception);
        }
    }

    private List<DailyRoomStatusDayVO> buildDays(LocalDate firstDate, List<RoomEntity> rooms,
            List<OrderViewEntity> orders) {
        Map<Long, List<OrderViewEntity>> ordersByRoom = orders.stream()
                .collect(Collectors.groupingBy(OrderViewEntity::getRoomId));
        List<DailyRoomStatusDayVO> days = new ArrayList<>();
        for (int offset = 0; offset < REPORT_DAY_COUNT; offset++) {
            LocalDate date = firstDate.plusDays(offset);
            List<DailyRoomStatusRowVO> rows = new ArrayList<>();
            for (RoomEntity room : rooms) {
                List<OrderViewEntity> roomOrders = ordersByRoom.getOrDefault(room.getId(), List.of()).stream()
                        .filter(order -> appliesToDate(order, date))
                        .toList();
                if (roomOrders.isEmpty()) {
                    rows.add(emptyRoom(room));
                } else {
                    roomOrders.forEach(order -> rows.add(orderRow(room, order, date)));
                }
            }
            days.add(new DailyRoomStatusDayVO(DATE_FORMATTER.format(date), DAY_NAMES.get(offset), rows));
        }
        return days;
    }

    private boolean appliesToDate(OrderViewEntity order, LocalDate date) {
        return !order.getCheckIn().isAfter(date) && !order.getCheckOut().isBefore(date);
    }

    private DailyRoomStatusRowVO emptyRoom(RoomEntity room) {
        return new DailyRoomStatusRowVO(room.getNumber(), room.getName(), "空房", "—", "—", "—", "—", "—",
                null, "—", "—", null, "—", "—");
    }

    private DailyRoomStatusRowVO orderRow(RoomEntity room, OrderViewEntity order, LocalDate date) {
        return new DailyRoomStatusRowVO(room.getNumber(), room.getName(), roomStatus(order, date),
                orderStatus(order.getStatus()), order.getOrderNo(), order.getChannelName(), order.getGuestName(),
                order.getPhone(), order.getGuestCount(), DATE_FORMATTER.format(order.getCheckIn()),
                DATE_FORMATTER.format(order.getCheckOut()), order.getNights(), display(order.getRemark()),
                display(order.getAdminRemark()));
    }

    private String roomStatus(OrderViewEntity order, LocalDate date) {
        if (order.getCheckIn().isEqual(date)) {
            return "入住";
        }
        if (order.getCheckOut().isEqual(date)) {
            return "退房";
        }
        return "续住";
    }

    private String orderStatus(OrderStatus status) {
        return switch (status) {
            case PENDING_CONFIRM -> "待确认（PENDING_CONFIRM）";
            case PENDING_CHECKIN -> "待入住（PENDING_CHECKIN）";
            case CHECKED_IN -> "已入住（CHECKED_IN）";
            case COMPLETED -> "已完成（COMPLETED）";
            case CANCELLED -> "已取消（CANCELLED）";
        };
    }

    private String display(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}
