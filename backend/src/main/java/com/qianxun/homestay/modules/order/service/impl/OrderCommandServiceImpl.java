package com.qianxun.homestay.modules.order.service.impl;

import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.common.util.ApiTime;
import com.qianxun.homestay.modules.availability.service.AvailabilityService;
import com.qianxun.homestay.modules.channel.entity.ChannelEntity;
import com.qianxun.homestay.modules.channel.mapper.ChannelMapper;
import com.qianxun.homestay.modules.creator.entity.CreatorEntity;
import com.qianxun.homestay.modules.creator.mapper.CreatorMapper;
import com.qianxun.homestay.modules.order.domain.OrderAmountCalculator;
import com.qianxun.homestay.modules.order.domain.OrderNoGenerator;
import com.qianxun.homestay.modules.order.domain.OrderStateMachine;
import com.qianxun.homestay.modules.order.dto.CancelOrderRequest;
import com.qianxun.homestay.modules.order.dto.CreateExternalOrderRequest;
import com.qianxun.homestay.modules.order.dto.OrderAmountRequest;
import com.qianxun.homestay.modules.order.dto.UpdateOrderRequest;
import com.qianxun.homestay.modules.order.entity.OrderAmountEntity;
import com.qianxun.homestay.modules.order.entity.OrderEntity;
import com.qianxun.homestay.modules.order.entity.OrderViewEntity;
import com.qianxun.homestay.modules.order.enums.OrderAction;
import com.qianxun.homestay.modules.order.enums.OrderStatus;
import com.qianxun.homestay.modules.order.mapper.OrderAmountMapper;
import com.qianxun.homestay.modules.order.mapper.OrderMapper;
import com.qianxun.homestay.modules.order.service.OrderCommandService;
import com.qianxun.homestay.modules.order.service.OrderRewardService;
import com.qianxun.homestay.modules.order.vo.OrderCreatedVO;
import com.qianxun.homestay.modules.order.vo.OrderDetailVO;
import com.qianxun.homestay.modules.order.vo.OrderStatusVO;
import com.qianxun.homestay.modules.room.entity.RoomEntity;
import com.qianxun.homestay.modules.room.mapper.RoomMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** 在事务中维护第三方订单、金额快照和逐级状态流转。 */
@Service
public class OrderCommandServiceImpl implements OrderCommandService {

    private static final Long MINI_PROGRAM_CHANNEL_ID = 1L;
    private static final long FIRST_EXTERNAL_CHANNEL_ID = 2L;
    private static final long LAST_EXTERNAL_CHANNEL_ID = 7L;
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final List<OrderStatus> EXTERNAL_INITIAL_STATUSES = List.of(
            OrderStatus.PENDING_CONFIRM, OrderStatus.PENDING_CHECKIN, OrderStatus.CHECKED_IN,
            OrderStatus.COMPLETED);

    private final OrderMapper orderMapper;
    private final OrderAmountMapper orderAmountMapper;
    private final RoomMapper roomMapper;
    private final ChannelMapper channelMapper;
    private final CreatorMapper creatorMapper;
    private final AvailabilityService availabilityService;
    private final OrderNoGenerator orderNoGenerator;
    private final OrderRewardService orderRewardService;
    private final OrderAmountCalculator amountCalculator = new OrderAmountCalculator();
    private final OrderStateMachine stateMachine = new OrderStateMachine();

    public OrderCommandServiceImpl(
            OrderMapper orderMapper,
            OrderAmountMapper orderAmountMapper,
            RoomMapper roomMapper,
            ChannelMapper channelMapper,
            CreatorMapper creatorMapper,
            AvailabilityService availabilityService,
            OrderNoGenerator orderNoGenerator,
            OrderRewardService orderRewardService) {
        this.orderMapper = orderMapper;
        this.orderAmountMapper = orderAmountMapper;
        this.roomMapper = roomMapper;
        this.channelMapper = channelMapper;
        this.creatorMapper = creatorMapper;
        this.availabilityService = availabilityService;
        this.orderNoGenerator = orderNoGenerator;
        this.orderRewardService = orderRewardService;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public OrderCreatedVO createExternalOrder(CreateExternalOrderRequest request) {
        availabilityService.validateStayRange(request.checkIn(), request.checkOut());
        availabilityService.lockRoomsForUpdate(List.of(request.roomId()));
        ChannelEntity channel = requireEnabledChannel(request.channelId());
        if (channel.getId() < FIRST_EXTERNAL_CHANNEL_ID || channel.getId() > LAST_EXTERNAL_CHANNEL_ID) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "只能手工录入六个外部渠道订单");
        }
        RoomEntity room = requireRoom(request.roomId());
        validateGuestCount(request.guestCount(), room);
        CreatorEntity creator = validateCreator(request.creatorId(), request.channelId());

        String platformOrderNo = trimToNull(request.platformOrderNo());
        if (platformOrderNo != null && orderMapper.existsPlatformOrderNo(request.channelId(), platformOrderNo)) {
            throw new BusinessException(HttpStatus.CONFLICT, "该渠道平台订单号已存在");
        }
        if (!EXTERNAL_INITIAL_STATUSES.contains(request.status())) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "第三方订单不能直接创建为已取消状态");
        }
        if (availabilityService.hasConflict(request.roomId(), request.checkIn(), request.checkOut(), null)) {
            throw new BusinessException(HttpStatus.CONFLICT, "房间在所选住宿区间不可预订");
        }

        OrderEntity order = new OrderEntity();
        order.setOrderNo(orderNoGenerator.nextOrderNo());
        order.setPlatformOrderNo(platformOrderNo);
        order.setRoomId(request.roomId());
        order.setChannelId(request.channelId());
        order.setCreatorId(creator == null ? null : creator.getId());
        order.setMemberId(null);
        order.setGuestName(trimToNull(request.guestName()));
        order.setPhone(trimToNull(request.phone()));
        order.setGuestCount(request.guestCount());
        order.setCheckIn(request.checkIn());
        order.setCheckOut(request.checkOut());
        order.setStatus(request.status());
        order.setRemark(trimToNull(request.remark()));

        OrderAmountEntity amount = amountCalculator.calculate(request.amount(), null, creator != null);
        try {
            orderMapper.insert(order);
            amount.setOrderId(order.getId());
            orderAmountMapper.insert(amount);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(HttpStatus.CONFLICT, "平台订单号或订单编号已存在");
        }
        if (order.getStatus() == OrderStatus.COMPLETED) {
            orderRewardService.rewardIfEligible(order.getId());
        }
        OrderAmountEntity savedAmount = orderAmountMapper.findByOrderId(order.getId());
        return new OrderCreatedVO(order.getId(), order.getOrderNo(), money(savedAmount.getNetAmount()));
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public OrderDetailVO updateOrder(Long orderId, UpdateOrderRequest request) {
        rejectUnknownUpdateFields(request.getUnknownFields());
        OrderEntity initial = requireOrder(orderId);
        List<Long> roomIds = new ArrayList<>();
        roomIds.add(initial.getRoomId());
        if (request.getRoomId() != null) {
            roomIds.add(request.getRoomId());
        }
        availabilityService.lockRoomsForUpdate(roomIds);
        OrderEntity order = requireOrderForUpdate(orderId);
        if (!order.getRoomId().equals(initial.getRoomId())) {
            throw new BusinessException(HttpStatus.CONFLICT, "订单已被修改，请刷新后重试");
        }

        boolean adjustsStay = request.getRoomId() != null || request.getCheckIn() != null
                || request.getCheckOut() != null;
        if (adjustsStay && order.getStatus() != OrderStatus.PENDING_CONFIRM
                && order.getStatus() != OrderStatus.PENDING_CHECKIN) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "当前订单状态不允许调整房间或日期");
        }

        Long roomId = request.getRoomId() == null ? order.getRoomId() : request.getRoomId();
        LocalDate checkIn = request.getCheckIn() == null ? order.getCheckIn() : request.getCheckIn();
        LocalDate checkOut = request.getCheckOut() == null ? order.getCheckOut() : request.getCheckOut();
        if (adjustsStay) {
            availabilityService.validateStayRange(checkIn, checkOut);
            long oldNights = order.getCheckOut().toEpochDay() - order.getCheckIn().toEpochDay();
            long newNights = checkOut.toEpochDay() - checkIn.toEpochDay();
            if (oldNights != newNights) {
                throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "改期前后住宿晚数必须一致");
            }
            RoomEntity room = requireRoom(roomId);
            validateGuestCount(request.getGuestCount() == null ? order.getGuestCount() : request.getGuestCount(), room);
            if (availabilityService.hasConflictExcludingOrder(roomId, checkIn, checkOut, orderId)) {
                throw new BusinessException(HttpStatus.CONFLICT, "房间在所选住宿区间不可预订");
            }
        } else if (request.getGuestCount() != null) {
            validateGuestCount(request.getGuestCount(), requireRoom(order.getRoomId()));
        }

        order.setRoomId(roomId);
        order.setCheckIn(checkIn);
        order.setCheckOut(checkOut);
        if (request.getGuestName() != null) {
            order.setGuestName(trimToNull(request.getGuestName()));
        }
        if (request.getPhone() != null) {
            order.setPhone(trimToNull(request.getPhone()));
        }
        if (request.getGuestCount() != null) {
            order.setGuestCount(request.getGuestCount());
        }
        if (request.getRemark() != null) {
            order.setRemark(trimToNull(request.getRemark()));
        }
        orderMapper.updateEditableFields(order);
        if (order.getStatus() == OrderStatus.COMPLETED) {
            orderRewardService.rewardIfEligible(orderId);
        }
        return findDetailAfterWrite(orderId);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public OrderStatusVO performAction(Long orderId, OrderAction action, CancelOrderRequest request) {
        OrderEntity initial = requireOrder(orderId);
        availabilityService.lockRoomsForUpdate(List.of(initial.getRoomId()));
        OrderEntity order = requireOrderForUpdate(orderId);
        if (!order.getRoomId().equals(initial.getRoomId())) {
            throw new BusinessException(HttpStatus.CONFLICT, "订单已被修改，请刷新后重试");
        }

        OrderStatus nextStatus;
        if (action == OrderAction.CANCEL) {
            if (MINI_PROGRAM_CHANNEL_ID.equals(order.getChannelId())) {
                throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "小程序订单须通过顾客退款流程取消");
            }
            nextStatus = stateMachine.transition(order.getStatus(), action);
            String reason = request == null ? null : trimToNull(request.reason());
            if (reason != null) {
                String suffix = (order.getRemark() == null ? "" : "\n") + "取消原因：" + reason;
                String updatedRemark = (order.getRemark() == null ? "" : order.getRemark()) + suffix;
                if (updatedRemark.length() > 500) {
                    throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "备注与取消原因合计不能超过 500 个字符");
                }
                order.setRemark(updatedRemark);
                orderMapper.updateEditableFields(order);
            }
        } else {
            nextStatus = stateMachine.transition(order.getStatus(), action);
            LocalDate today = LocalDate.now(BUSINESS_ZONE);
            if (action == OrderAction.CHECK_IN && today.isBefore(order.getCheckIn())) {
                throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "不能早于预定入住日办理入住");
            }
            if (action == OrderAction.CHECK_OUT && today.isBefore(order.getCheckOut())) {
                throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "不能早于预定退房日办理退房");
            }
        }

        orderMapper.updateStatus(orderId, nextStatus.name());
        if (nextStatus == OrderStatus.COMPLETED) {
            // 完成状态与积分奖励共用事务；奖励失败则状态也回滚，避免漏发。
            orderRewardService.rewardIfEligible(orderId);
        }
        OrderViewEntity updated = orderMapper.findViewById(orderId);
        return new OrderStatusVO(updated.getId(), updated.getStatus(), ApiTime.fromShanghai(updated.getUpdatedAt()));
    }

    private OrderDetailVO findDetailAfterWrite(Long orderId) {
        OrderViewEntity order = orderMapper.findViewById(orderId);
        if (order == null) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "订单保存后读取失败");
        }
        return new OrderDetailVO(
                order.getId(), order.getOrderNo(), order.getPlatformOrderNo(), order.getRoomId(), order.getRoomNumber(),
                order.getChannelId(), order.getChannelName(), order.getCreatorId(), order.getMemberId(),
                order.getGuestName(), order.getPhone(), order.getGuestCount(), order.getCheckIn(), order.getCheckOut(),
                order.getNights(), order.getStatus(), order.getRemark(), money(order.getPaidAmount()),
                money(order.getPlatformFee()), money(order.getPlatformCommission()), money(order.getExtraCommission()),
                nullableMoney(order.getExpectedAmount()), money(order.getNetAmount()),
                ApiTime.fromShanghai(order.getCreatedAt()), ApiTime.fromShanghai(order.getUpdatedAt()));
    }

    private OrderEntity requireOrder(Long orderId) {
        OrderEntity order = orderMapper.findById(orderId);
        if (order == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "订单不存在");
        }
        return order;
    }

    private OrderEntity requireOrderForUpdate(Long orderId) {
        OrderEntity order = orderMapper.findByIdForUpdate(orderId);
        if (order == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "订单不存在");
        }
        return order;
    }

    private ChannelEntity requireEnabledChannel(Long channelId) {
        ChannelEntity channel = channelMapper.findById(channelId);
        if (channel == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "订单渠道不存在");
        }
        if (!Boolean.TRUE.equals(channel.getEnabled())) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "该渠道已停用，不能创建新订单");
        }
        return channel;
    }

    private RoomEntity requireRoom(Long roomId) {
        RoomEntity room = roomMapper.findById(roomId);
        if (room == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "房间不存在");
        }
        return room;
    }

    private CreatorEntity validateCreator(Long creatorId, Long channelId) {
        if (creatorId == null) {
            return null;
        }
        CreatorEntity creator = creatorMapper.findById(creatorId);
        if (creator == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "达人不存在");
        }
        if (!Boolean.TRUE.equals(creator.getEnabled())) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "该达人已停用，不能用于新订单");
        }
        if (!channelId.equals(creator.getChannelId())) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "达人所属渠道与订单渠道不一致");
        }
        return creator;
    }

    private void validateGuestCount(Integer guestCount, RoomEntity room) {
        if (guestCount != null && guestCount > room.getCapacity()) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "入住人数超过房间可住人数");
        }
    }

    private void rejectUnknownUpdateFields(Map<String, Object> unknownFields) {
        if (unknownFields.isEmpty()) {
            return;
        }
        if (unknownFields.keySet().stream().anyMatch(this::isAmountField)) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "订单创建后金额及费用字段不可修改");
        }
        throw new BusinessException(HttpStatus.BAD_REQUEST, "请求包含未定义字段");
    }

    private boolean isAmountField(String field) {
        return switch (field) {
            case "amount", "paid_amount", "platform_fee", "platform_commission", "extra_commission",
                    "expected_amount", "net_amount" -> true;
            default -> false;
        };
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String money(BigDecimal value) {
        return value.setScale(2, RoundingMode.UNNECESSARY).toPlainString();
    }

    private String nullableMoney(BigDecimal value) {
        return value == null ? null : money(value);
    }
}
