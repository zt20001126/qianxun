package com.qianxun.homestay.modules.roomblock.controller;

import com.qianxun.homestay.common.api.ApiResponse;
import com.qianxun.homestay.common.api.ListResponse;
import com.qianxun.homestay.modules.roomblock.dto.RoomBlockSaveRequest;
import com.qianxun.homestay.modules.roomblock.service.RoomBlockService;
import com.qianxun.homestay.modules.roomblock.vo.RoomBlockVO;
import jakarta.validation.Valid;
import java.time.YearMonth;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** R10-R13 管理后台封房维护接口。 */
@RestController
@RequestMapping("/api/admin/room-blocks")
public class AdminRoomBlockController {

    private final RoomBlockService roomBlockService;

    public AdminRoomBlockController(RoomBlockService roomBlockService) {
        this.roomBlockService = roomBlockService;
    }

    @GetMapping
    public ApiResponse<ListResponse<RoomBlockVO>> list(
            @RequestParam(name = "room_id", required = false) Long roomId,
            @RequestParam(name = "month", required = false)
            @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return ApiResponse.success(new ListResponse<>(roomBlockService.findRoomBlocks(roomId, month)));
    }

    @PostMapping
    public ApiResponse<RoomBlockVO> create(@Valid @RequestBody RoomBlockSaveRequest request) {
        return ApiResponse.success(roomBlockService.createRoomBlock(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<RoomBlockVO> update(
            @PathVariable Long id, @Valid @RequestBody RoomBlockSaveRequest request) {
        return ApiResponse.success(roomBlockService.updateRoomBlock(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        roomBlockService.deleteRoomBlock(id);
        return ApiResponse.success(null);
    }
}
