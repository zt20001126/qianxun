package com.qianxun.homestay.modules.notification.service;

import com.qianxun.homestay.modules.notification.event.OrderNotificationEvent;

/** 向管理员发送订单邮件通知。 */
public interface NotificationService {
    /** 根据已提交的业务事件渲染并发送管理员邮件。 */
    void send(OrderNotificationEvent event);
}
