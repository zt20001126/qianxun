package com.qianxun.homestay.modules.room.controller;

import com.qianxun.homestay.common.api.ApiResponse;
import com.qianxun.homestay.common.api.ListResponse;
import com.qianxun.homestay.modules.room.service.RoomService;
import com.qianxun.homestay.modules.room.vo.AppRoomDetailVO;
import com.qianxun.homestay.modules.room.vo.AppRoomSummaryVO;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** R01/R02 小程序房间浏览接口。 */
@RestController
@RequestMapping("/api/app/rooms")
public class AppRoomController {

    private final RoomService roomService;

    public AppRoomController(RoomService roomService) {
        this.roomService = roomService;
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
}
