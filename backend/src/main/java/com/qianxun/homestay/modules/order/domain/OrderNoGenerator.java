package com.qianxun.homestay.modules.order.domain;

import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.modules.order.mapper.OrderMapper;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** 锁定单管理员记录串行分配订单号日流水，不依赖无锁 MAX+1。 */
@Component
public class OrderNoGenerator {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;
    private final OrderMapper orderMapper;

    public OrderNoGenerator(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    /** 生成当天唯一的 QX 订单号；超出四位时自动扩展流水位数。 */
    public String nextOrderNo() {
        String date = LocalDate.now(BUSINESS_ZONE).format(DATE_FORMAT);
        if (orderMapper.lockOrderNumberGeneration() == null) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "订单编号暂不可用，请重试");
        }
        Long lastSequence = orderMapper.findMaxOrderSequence("QX" + date + "%");
        long nextSequence = (lastSequence == null ? 0L : lastSequence) + 1L;
        return "QX" + date + String.format(Locale.ROOT, "%04d", nextSequence);
    }
}
