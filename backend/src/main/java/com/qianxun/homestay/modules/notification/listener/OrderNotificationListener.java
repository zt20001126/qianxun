package com.qianxun.homestay.modules.notification.listener;

import com.qianxun.homestay.modules.notification.event.OrderNotificationEvent;
import com.qianxun.homestay.modules.notification.service.NotificationService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** 仅在订单事务提交后触发邮件，避免回滚业务产生错误通知。 */
@Component
public class OrderNotificationListener {
    private final NotificationService service;
    public OrderNotificationListener(NotificationService service) { this.service = service; }
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderChanged(OrderNotificationEvent event) { service.send(event); }
}
