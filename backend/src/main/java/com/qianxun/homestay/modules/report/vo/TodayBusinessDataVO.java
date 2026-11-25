package com.qianxun.homestay.modules.report.vo;
import java.time.LocalDate;
/** 日看板的房间和经营金额汇总。 */
public record TodayBusinessDataVO(
        /** 北京时间统计日期。 */ LocalDate date,
        /** 房间总数。 */ int totalRooms,
        /** 订单占房与封房之和。 */ int occupiedRooms,
        /** 可用空房数。 */ int availableRooms,
        /** 日看板入住率，百分数格式。 */ String occupancyRate,
        /** 当晚分摊实付金额。 */ String paidAmount,
        /** 当晚分摊平台手续费。 */ String platformFee,
        /** 当晚分摊平台达人提成。 */ String platformCommission,
        /** 当晚分摊达人额外提成。 */ String extraCommission,
        /** 当晚单独分摊的商家到手金额。 */ String netAmount) {}
