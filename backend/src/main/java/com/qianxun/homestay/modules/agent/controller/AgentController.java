package com.qianxun.homestay.modules.agent.controller;
import com.qianxun.homestay.common.api.ApiResponse;
import com.qianxun.homestay.common.security.AuthPrincipal;
import com.qianxun.homestay.modules.agent.dto.AgentChatRequest;
import com.qianxun.homestay.modules.agent.service.AgentService;
import com.qianxun.homestay.modules.agent.vo.*;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
/** 后台智能体对话与会话查询接口。 */
@RestController
@RequestMapping("/api/admin/agent")
public class AgentController {
    private final AgentService service;
    public AgentController(AgentService service) { this.service = service; }
    /** 发送消息并返回回答或待答问题。 */
    @PostMapping("/chat") public ApiResponse<AgentChatVO> chat(@AuthenticationPrincipal AuthPrincipal principal, @Valid @RequestBody AgentChatRequest request) {
        return ApiResponse.success(service.chat(principal.id(), request));
    }
    /** 分页查询当前管理员会话。 */
    @GetMapping("/conversations") public ApiResponse<AgentPageVO<AgentConversationVO>> conversations(@AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="20") int size) {
        return ApiResponse.success(service.conversations(principal.id(), page, size));
    }
    /** 分页查询指定会话消息。 */
    @GetMapping("/conversations/{id}/messages") public ApiResponse<AgentPageVO<AgentMessageVO>> messages(@AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long id, @RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="20") int size) {
        return ApiResponse.success(service.messages(principal.id(), id, page, size));
    }
}
