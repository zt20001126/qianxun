package com.qianxun.homestay.modules.room.controller;

import com.qianxun.homestay.common.api.ApiResponse;
import com.qianxun.homestay.modules.room.vo.RoomImageUploadVO;
import com.qianxun.homestay.modules.storage.service.CatalogImageUploadService;
import com.qianxun.homestay.modules.storage.service.CatalogImageUploadService.Target;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 提供后台客房图片上传接口。 */
@RestController
@RequestMapping("/api/admin/rooms/images")
public class AdminRoomImageController {
    private final CatalogImageUploadService imageUploadService;

    public AdminRoomImageController(CatalogImageUploadService imageUploadService) {
        this.imageUploadService = imageUploadService;
    }

    /** 上传单张图片，返回可在新增或编辑客房时保存的地址。 */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<RoomImageUploadVO> upload(@RequestPart("file") MultipartFile file) {
        return ApiResponse.success(new RoomImageUploadVO(imageUploadService.upload(file, Target.ROOM)));
    }
}
