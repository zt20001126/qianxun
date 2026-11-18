package com.qianxun.homestay.modules.room.controller;

import com.qianxun.homestay.common.api.ApiResponse;
import com.qianxun.homestay.common.api.ListResponse;
import com.qianxun.homestay.modules.availability.dto.AppAvailabilityCheckRequest;
import com.qianxun.homestay.modules.availability.service.AvailabilityService;
import com.qianxun.homestay.modules.availability.vo.AppAvailabilityCheckVO;
import com.qianxun.homestay.modules.availability.vo.AppRoomAvailabilityVO;
import com.qianxun.homestay.modules.room.service.RoomService;
import com.qianxun.homestay.modules.room.vo.AppRoomDetailVO;
import com.qianxun.homestay.modules.room.vo.AppRoomSummaryVO;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.YearMonth;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** R01-R04 小程序客房浏览和可订查询接口。 */
@RestController
@RequestMapping("/api/app/rooms")
public class AppRoomController {

    private final RoomService roomService;
    private final AvailabilityService availabilityService;

    public AppRoomController(RoomService roomService, AvailabilityService availabilityService) {
        this.roomService = roomService;
        this.availabilityService = availabilityService;
    }

    @GetMapping
    public ApiResponse<ListResponse<AppRoomSummaryVO>> list(
            @RequestParam(name = "check_in", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam(name = "check_out", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut) {
        return ApiResponse.success(new ListResponse<>(roomService.findAppRooms(checkIn, checkOut)));
    }

    @GetMapping("/{id}")
    public ApiResponse<AppRoomDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.success(roomService.getAppRoom(id));
    }

    @GetMapping("/{id}/availability")
    public ApiResponse<AppRoomAvailabilityVO> availability(
            @PathVariable Long id,
            @RequestParam(name = "month") @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return ApiResponse.success(availabilityService.getAppRoomAvailability(id, month));
    }

    @PostMapping("/availability-check")
    public ApiResponse<AppAvailabilityCheckVO> checkAvailability(
            @Valid @RequestBody AppAvailabilityCheckRequest request) {
        return ApiResponse.success(availabilityService.checkAppAvailability(
                request.roomId(), request.checkIn(), request.checkOut()));
    }
}
