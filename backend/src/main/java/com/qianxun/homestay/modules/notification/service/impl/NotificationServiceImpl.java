package com.qianxun.homestay.modules.notification.service.impl;

import com.qianxun.homestay.modules.mall.entity.MallOrderEntity;
import com.qianxun.homestay.modules.mall.mapper.MallOrderMapper;
import com.qianxun.homestay.modules.notification.config.MailNotificationProperties;
import com.qianxun.homestay.modules.notification.event.OrderNotificationEvent;
import com.qianxun.homestay.modules.notification.service.NotificationService;
import com.qianxun.homestay.modules.order.entity.OrderViewEntity;
import com.qianxun.homestay.modules.order.mapper.OrderMapper;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

/** 渲染 HTML 模板并向配置的管理员邮箱发送订单通知。 */
@Service
public class NotificationServiceImpl implements NotificationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationServiceImpl.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final MailNotificationProperties properties; private final JavaMailSender sender;
    private final SpringTemplateEngine templates; private final OrderMapper orders; private final MallOrderMapper mallOrders;
    public NotificationServiceImpl(MailNotificationProperties properties, JavaMailSender sender, SpringTemplateEngine templates,
            OrderMapper orders, MallOrderMapper mallOrders) { this.properties = properties; this.sender = sender; this.templates = templates; this.orders = orders; this.mallOrders = mallOrders; }
    @Override public void send(OrderNotificationEvent event) {
        if (properties.recipientList().isEmpty()) { LOGGER.warn("邮件通知未配置收件人，type={}, orderId={}", event.type(), event.orderId()); return; }
        try {
            Context context = new Context(); String template; String subject;
            if (event.type() == OrderNotificationEvent.Type.STAY_PAID || event.type() == OrderNotificationEvent.Type.STAY_CANCELLED) {
                OrderViewEntity order = orders.findViewById(event.orderId()); if (order == null) return;
                context.setVariable("order", order); context.setVariable("occurredAt", TIME_FORMATTER.format(event.occurredAt()));
                context.setVariable("refundAmount", event.refundAmount() == null ? null : money(event.refundAmount())); context.setVariable("reason", event.reason());
                boolean paid = event.type() == OrderNotificationEvent.Type.STAY_PAID; template = paid ? "mail/stay-order-paid" : "mail/stay-order-cancelled";
                subject = "[芊浔民宿] " + (paid ? "新住宿订单 " : "住宿订单已取消 ") + order.getOrderNo();
            } else {
                MallOrderEntity order = mallOrders.findById(event.orderId()); if (order == null) return;
                context.setVariable("order", order); context.setVariable("occurredAt", TIME_FORMATTER.format(event.occurredAt()));
                context.setVariable("refundAmount", event.refundAmount() == null ? null : money(event.refundAmount()));
                context.setVariable("unitPrice", money(order.getTotalAmount().divide(java.math.BigDecimal.valueOf(order.getQuantity()), 2, RoundingMode.HALF_UP)));
                boolean paid = event.type() == OrderNotificationEvent.Type.MALL_PAID; template = paid ? "mail/mall-order-paid" : "mail/mall-order-refunded";
                subject = "[芊浔民宿] " + (paid ? "新商城订单 " : "商城订单已退款 ") + order.getOrderNo();
            }
            String html = templates.process(template, context);
            for (String recipient : properties.recipientList()) { var message = sender.createMimeMessage(); var helper = new MimeMessageHelper(message, "UTF-8"); if (!properties.getFromEmail().isBlank()) helper.setFrom(properties.getFromEmail(), properties.getSenderName()); helper.setTo(recipient); helper.setSubject(subject); helper.setText(html, true); sender.send(message); }
        } catch (Exception exception) { LOGGER.error("订单邮件发送失败，type={}, orderId={}", event.type(), event.orderId(), exception); }
    }
    private String money(java.math.BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP).toPlainString(); }
}
