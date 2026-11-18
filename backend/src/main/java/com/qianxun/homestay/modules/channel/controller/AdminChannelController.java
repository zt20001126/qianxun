package com.qianxun.homestay.modules.channel.controller;

import com.qianxun.homestay.common.api.ApiResponse;
import com.qianxun.homestay.common.api.ListResponse;
import com.qianxun.homestay.modules.channel.dto.ChannelEnabledRequest;
import com.qianxun.homestay.modules.channel.service.ChannelService;
import com.qianxun.homestay.modules.channel.vo.ChannelVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** C01/C02 后台渠道管理接口。 */
@RestController
@RequestMapping("/api/admin/channels")
public class AdminChannelController {

    private final ChannelService channelService;

    public AdminChannelController(ChannelService channelService) {
        this.channelService = channelService;
    }

    @GetMapping
    public ApiResponse<ListResponse<ChannelVO>> list() {
        return ApiResponse.success(new ListResponse<>(channelService.findChannels()));
    }

    @PutMapping("/{id}/enabled")
    public ApiResponse<ChannelVO> setEnabled(
            @PathVariable Long id,
            @Valid @RequestBody ChannelEnabledRequest request) {
        return ApiResponse.success(channelService.setEnabled(id, request.enabled()));
    }
}
