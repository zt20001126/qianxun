package com.qianxun.homestay.modules.creator.controller;

import com.qianxun.homestay.common.api.ApiResponse;
import com.qianxun.homestay.common.api.ListResponse;
import com.qianxun.homestay.modules.creator.dto.CreatorEnabledRequest;
import com.qianxun.homestay.modules.creator.dto.CreatorQuery;
import com.qianxun.homestay.modules.creator.dto.CreatorSaveRequest;
import com.qianxun.homestay.modules.creator.service.CreatorService;
import com.qianxun.homestay.modules.creator.vo.CreatorVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** D01-D05 后台达人管理接口。 */
@RestController
@RequestMapping("/api/admin/creators")
public class AdminCreatorController {

    private final CreatorService creatorService;

    public AdminCreatorController(CreatorService creatorService) {
        this.creatorService = creatorService;
    }

    @GetMapping
    public ApiResponse<ListResponse<CreatorVO>> list(
            @RequestParam(name = "channel_id", required = false) Long channelId,
            @RequestParam(name = "enabled", required = false) Boolean enabled,
            @RequestParam(name = "name", required = false) String name) {
        return ApiResponse.success(new ListResponse<>(creatorService.findCreators(new CreatorQuery(channelId, enabled, name))));
    }

    @GetMapping("/{id}")
    public ApiResponse<CreatorVO> detail(@PathVariable Long id) {
        return ApiResponse.success(creatorService.getCreator(id));
    }

    @PostMapping
    public ApiResponse<CreatorVO> create(@Valid @RequestBody CreatorSaveRequest request) {
        return ApiResponse.success(creatorService.createCreator(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<CreatorVO> update(@PathVariable Long id, @Valid @RequestBody CreatorSaveRequest request) {
        return ApiResponse.success(creatorService.updateCreator(id, request));
    }

    @PutMapping("/{id}/enabled")
    public ApiResponse<CreatorVO> setEnabled(
            @PathVariable Long id,
            @Valid @RequestBody CreatorEnabledRequest request) {
        return ApiResponse.success(creatorService.setEnabled(id, request.enabled()));
    }
}
