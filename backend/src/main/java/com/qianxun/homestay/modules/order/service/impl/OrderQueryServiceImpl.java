package com.qianxun.homestay.modules.order.service.impl;

import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.common.util.ApiTime;
import com.qianxun.homestay.modules.order.dto.OrderQueryRequest;
import com.qianxun.homestay.modules.order.entity.OrderViewEntity;
import com.qianxun.homestay.modules.order.mapper.OrderMapper;
import com.qianxun.homestay.modules.order.service.OrderQueryService;
import com.qianxun.homestay.modules.order.vo.OrderDetailVO;
import com.qianxun.homestay.modules.order.vo.OrderListItemVO;
import com.qianxun.homestay.modules.order.vo.OrderPageVO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 执行后台订单分页与详情读取，并将数据库金额转为字符串。 */
@Service
public class OrderQueryServiceImpl implements OrderQueryService {

    private final OrderMapper orderMapper;

    public OrderQueryServiceImpl(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public OrderPageVO findOrders(OrderQueryRequest query) {
        if (query.checkInFrom() != null && query.checkInTo() != null
                && query.checkInFrom().isAfter(query.checkInTo())) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "入住日期起始值不能晚于结束值");
        }
        long offset = (long) (query.page() - 1) * query.size();
        List<OrderListItemVO> list = orderMapper.findPage(query, offset).stream().map(this::toListItem).toList();
        return new OrderPageVO(list, orderMapper.count(query), query.page(), query.size());
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDetailVO findOrder(Long orderId) {
        OrderViewEntity order = orderMapper.findViewById(orderId);
        if (order == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "订单不存在");
        }
        return toDetail(order);
    }

    private OrderListItemVO toListItem(OrderViewEntity order) {
        return new OrderListItemVO(
                order.getId(), order.getOrderNo(), order.getRoomId(), order.getRoomNumber(), order.getChannelId(),
                order.getCreatorId(), order.getGuestName(), order.getPhone(), order.getCheckIn(), order.getCheckOut(),
                order.getNights(), order.getStatus(), money(order.getPaidAmount()), money(order.getPlatformFee()),
                money(order.getPlatformCommission()), money(order.getExtraCommission()),
                nullableMoney(order.getExpectedAmount()), money(order.getNetAmount()));
    }

    private OrderDetailVO toDetail(OrderViewEntity order) {
        return new OrderDetailVO(
                order.getId(), order.getOrderNo(), order.getPlatformOrderNo(), order.getRoomId(), order.getRoomNumber(),
                order.getChannelId(), order.getChannelName(), order.getCreatorId(), order.getMemberId(),
                order.getGuestName(), order.getPhone(), order.getGuestCount(), order.getCheckIn(), order.getCheckOut(),
                order.getNights(), order.getStatus(), order.getRemark(), money(order.getPaidAmount()),
                money(order.getPlatformFee()), money(order.getPlatformCommission()), money(order.getExtraCommission()),
                nullableMoney(order.getExpectedAmount()), money(order.getNetAmount()),
                ApiTime.fromShanghai(order.getCreatedAt()), ApiTime.fromShanghai(order.getUpdatedAt()));
    }

    private String money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.UNNECESSARY).toPlainString();
    }

    private String nullableMoney(BigDecimal amount) {
        return amount == null ? null : money(amount);
    }
}
