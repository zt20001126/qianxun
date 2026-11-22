package com.qianxun.homestay.modules.order.domain;

import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.modules.order.mapper.OrderMapper;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 锁定单管理员记录，串行分配住宿及商城共用的订单号日流水。 */
@Component
public class OrderNoGenerator {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;
    private final OrderMapper orderMapper;

    public OrderNoGenerator(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    /** 生成当天唯一的 QX 订单号；超出四位时自动扩展流水位数。 */
    @Transactional(propagation = Propagation.MANDATORY)
    public String nextOrderNo() {
        String date = LocalDate.now(BUSINESS_ZONE).format(DATE_FORMAT);
        if (orderMapper.lockOrderNumberGeneration() == null) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "订单编号暂不可用，请重试");
        }
        // 两个查询都使用当前读，避免等待管理员锁后沿用事务开始时的旧快照。
        String pattern = "QX" + date + "%";
        Long roomSequence = orderMapper.findMaxOrderSequence(pattern);
        Long mallSequence = orderMapper.findMaxMallOrderSequence(pattern);
        long nextSequence = Math.max(roomSequence == null ? 0L : roomSequence,
                mallSequence == null ? 0L : mallSequence) + 1L;
        return "QX" + date + String.format(Locale.ROOT, "%04d", nextSequence);
    }
}
