package com.qianxun.homestay.modules.notification.vo;

import java.util.List;

/** 每日房态邮件中一个业务日期的客房状态分区。 */
public class DailyRoomStatusDayVO {

    /** 业务日期文本。 */
    private final String date;
    /** 日期在邮件中的相对名称，例如今日、明日或后天。 */
    private final String dayName;
    /** 当日按房号排列的房态记录。 */
    private final List<DailyRoomStatusRowVO> rows;

    public DailyRoomStatusDayVO(String date, String dayName, List<DailyRoomStatusRowVO> rows) {
        this.date = date;
        this.dayName = dayName;
        this.rows = rows;
    }

    public String getDate() { return date; }
    public String getDayName() { return dayName; }
    public List<DailyRoomStatusRowVO> getRows() { return rows; }
}
