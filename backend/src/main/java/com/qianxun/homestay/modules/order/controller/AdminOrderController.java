package com.qianxun.homestay.modules.order.controller;

import com.qianxun.homestay.common.api.ApiResponse;
import com.qianxun.homestay.modules.order.dto.CancelOrderRequest;
import com.qianxun.homestay.modules.order.dto.CreateExternalOrderRequest;
import com.qianxun.homestay.modules.order.dto.OrderQueryRequest;
import com.qianxun.homestay.modules.order.dto.UpdateOrderRequest;
import com.qianxun.homestay.modules.order.enums.OrderAction;
import com.qianxun.homestay.modules.order.enums.OrderStatus;
import com.qianxun.homestay.modules.order.service.OrderCommandService;
import com.qianxun.homestay.modules.order.service.OrderQueryService;
import com.qianxun.homestay.modules.portrait.dto.BookerPortraitSaveRequest;
import com.qianxun.homestay.modules.order.vo.OrderCreatedVO;
import com.qianxun.homestay.modules.order.vo.OrderDetailVO;
import com.qianxun.homestay.modules.order.vo.OrderPageVO;
import com.qianxun.homestay.modules.order.vo.OrderStatusVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

/** O04-O10、O12 管理后台订单接口。 */
@RestController
@Validated
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final OrderQueryService orderQueryService;
    private final OrderCommandService orderCommandService;

    public AdminOrderController(OrderQueryService orderQueryService, OrderCommandService orderCommandService) {
        this.orderQueryService = orderQueryService;
        this.orderCommandService = orderCommandService;
    }

    @GetMapping
    public ApiResponse<OrderPageVO> list(
            @RequestParam(name = "channel_id", required = false) Long channelId,
            @RequestParam(name = "room_id", required = false) Long roomId,
            @RequestParam(name = "creator_id", required = false) Long creatorId,
            @RequestParam(name = "status", required = false) OrderStatus status,
            @RequestParam(name = "check_in_from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInFrom,
            @RequestParam(name = "check_in_to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInTo,
            @RequestParam(name = "guest_name", required = false) String guestName,
            @RequestParam(name = "phone", required = false) String phone,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "without_creator", defaultValue = "false") boolean withoutCreator,
            @RequestParam(name = "page", defaultValue = "1") @Min(1) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(orderQueryService.findOrders(new OrderQueryRequest(
                channelId, roomId, creatorId, status, checkInFrom, checkInTo,
                guestName, phone, page, size, keyword, withoutCreator)));
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.success(orderQueryService.findOrder(id));
    }

    @PostMapping
    public ApiResponse<OrderCreatedVO> create(@Valid @RequestBody CreateExternalOrderRequest request) {
        return ApiResponse.success(orderCommandService.createExternalOrder(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<OrderDetailVO> update(
            @PathVariable Long id, @Valid @RequestBody UpdateOrderRequest request) {
        return ApiResponse.success(orderCommandService.updateOrder(id, request));
    }

    @PostMapping("/{id}/confirm")
    public ApiResponse<OrderStatusVO> confirm(@PathVariable Long id) {
        return ApiResponse.success(orderCommandService.performAction(id, OrderAction.CONFIRM, null));
    }

    @PostMapping("/{id}/check-in")
    public ApiResponse<OrderStatusVO> checkIn(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) BookerPortraitSaveRequest portraitRequest) {
        return ApiResponse.success(orderCommandService.checkIn(id, portraitRequest));
    }

    @PostMapping("/{id}/check-out")
    public ApiResponse<OrderStatusVO> checkOut(@PathVariable Long id) {
        return ApiResponse.success(orderCommandService.performAction(id, OrderAction.CHECK_OUT, null));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<OrderStatusVO> cancel(
            @PathVariable Long id, @Valid @RequestBody(required = false) CancelOrderRequest request) {
        return ApiResponse.success(orderCommandService.performAction(id, OrderAction.CANCEL, request));
    }
}
