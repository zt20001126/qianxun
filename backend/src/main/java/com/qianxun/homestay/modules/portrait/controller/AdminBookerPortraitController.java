package com.qianxun.homestay.modules.portrait.controller;

import com.qianxun.homestay.common.api.ApiResponse;
import com.qianxun.homestay.modules.portrait.dto.BookerPortraitListQuery;
import com.qianxun.homestay.modules.portrait.dto.BookerPortraitUpdateRequest;
import com.qianxun.homestay.modules.portrait.dto.PortraitTagCreateRequest;
import com.qianxun.homestay.modules.portrait.service.BookerPortraitService;
import com.qianxun.homestay.modules.portrait.vo.BookerPortraitPageVO;
import com.qianxun.homestay.modules.portrait.vo.PortraitTagVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/** 管理端手机号画像查询和持续编辑接口。 */
@RestController
@Validated
@RequestMapping("/api/admin")
public class AdminBookerPortraitController {

    private final BookerPortraitService bookerPortraitService;

    public AdminBookerPortraitController(BookerPortraitService bookerPortraitService) {
        this.bookerPortraitService = bookerPortraitService;
    }

    /** 按姓名、手机号筛选顾客画像；画像身份按手机号归并。 */
    @GetMapping("/portraits")
    public ApiResponse<BookerPortraitPageVO> list(
            @RequestParam(name = "guest_name", required = false) String guestName,
            @RequestParam(name = "phone", required = false) String phone,
            @RequestParam(name = "page", defaultValue = "1") @Min(1) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(bookerPortraitService.findProfiles(
                new BookerPortraitListQuery(guestName, phone, page, size)));
    }

    /** 修改手机号画像和订单备注，不改变订单状态或金额。 */
    @PutMapping("/portraits/{id}")
    public ApiResponse<Void> update(
            @PathVariable Long id, @Valid @RequestBody BookerPortraitUpdateRequest request) {
        bookerPortraitService.updateProfile(id, request);
        return ApiResponse.success(null);
    }

    /** 查询预置及管理员新增的画像标签。 */
    @GetMapping("/portrait-tags")
    public ApiResponse<List<PortraitTagVO>> tags() {
        return ApiResponse.success(bookerPortraitService.findPortraitTags());
    }

    /** 新增供所有画像复用的标签。 */
    @PostMapping("/portrait-tags")
    public ApiResponse<PortraitTagVO> createTag(@Valid @RequestBody PortraitTagCreateRequest request) {
        return ApiResponse.success(bookerPortraitService.createPortraitTag(request));
    }
}
