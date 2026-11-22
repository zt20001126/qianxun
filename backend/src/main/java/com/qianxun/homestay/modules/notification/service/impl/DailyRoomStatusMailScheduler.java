package com.qianxun.homestay.modules.notification.service.impl;

import com.qianxun.homestay.modules.notification.service.DailyRoomStatusNotificationService;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 按北京时间在每日早晚触发三日房态邮件发送。 */
@Component
public class DailyRoomStatusMailScheduler {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final DailyRoomStatusNotificationService notificationService;

    public DailyRoomStatusMailScheduler(DailyRoomStatusNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /** 每日 10:00 和 18:00 发送以当日为起点的三日房态邮件。 */
    @Scheduled(cron = "${app.mail-notification.daily-room-status-cron:0 0 10,18 * * *}", zone = "Asia/Shanghai")
    public void sendDailyRoomStatus() {
        notificationService.sendDailyRoomStatus(LocalDate.now(BUSINESS_ZONE));
    }
}
