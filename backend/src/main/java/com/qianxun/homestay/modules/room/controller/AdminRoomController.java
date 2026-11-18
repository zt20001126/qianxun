package com.qianxun.homestay.modules.room.controller;

import com.qianxun.homestay.common.api.ApiResponse;
import com.qianxun.homestay.common.api.ListResponse;
import com.qianxun.homestay.modules.room.dto.RoomEnabledRequest;
import com.qianxun.homestay.modules.room.dto.RoomSaveRequest;
import com.qianxun.homestay.modules.room.service.RoomService;
import com.qianxun.homestay.modules.room.vo.RoomAdminVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** R05-R09 后台客房维护接口。 */
@RestController
@RequestMapping("/api/admin/rooms")
public class AdminRoomController {

    private final RoomService roomService;

    public AdminRoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping
    public ApiResponse<ListResponse<RoomAdminVO>> list() {
        return ApiResponse.success(new ListResponse<>(roomService.findAdminRooms()));
    }

    @GetMapping("/{id}")
    public ApiResponse<RoomAdminVO> detail(@PathVariable Long id) {
        return ApiResponse.success(roomService.getAdminRoom(id));
    }

    @PostMapping
    public ApiResponse<RoomAdminVO> create(@Valid @RequestBody RoomSaveRequest request) {
        return ApiResponse.success(roomService.createRoom(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<RoomAdminVO> update(@PathVariable Long id, @Valid @RequestBody RoomSaveRequest request) {
        return ApiResponse.success(roomService.updateRoom(id, request));
    }

    @PutMapping("/{id}/enabled")
    public ApiResponse<RoomAdminVO> setEnabled(
            @PathVariable Long id,
            @Valid @RequestBody RoomEnabledRequest request) {
        return ApiResponse.success(roomService.setRoomEnabled(id, request.enabled()));
    }
}
