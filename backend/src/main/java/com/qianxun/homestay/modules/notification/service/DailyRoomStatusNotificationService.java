package com.qianxun.homestay.modules.notification.service;

import java.time.LocalDate;

/** 向管理员发送连续三日客房入住安排邮件。 */
public interface DailyRoomStatusNotificationService {

    /**
     * 汇总指定日期起三天的客房状态并发送邮件。
     *
     * @param firstDate 邮件中的今日业务日期
     */
    void sendDailyRoomStatus(LocalDate firstDate);
}
