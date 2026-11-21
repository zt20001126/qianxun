package com.qianxun.homestay.modules.invitation.service.impl;

import com.qianxun.homestay.common.util.ApiTime;
import com.qianxun.homestay.modules.invitation.mapper.InvitationMapper;
import com.qianxun.homestay.modules.invitation.service.InvitationService;
import com.qianxun.homestay.modules.invitation.vo.InvitationPageVO;
import com.qianxun.homestay.modules.invitation.vo.InvitationRecordVO;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 在数据库约束保护下生成邀请标识并建立不可自动更换的关系。 */
@Service
public class InvitationServiceImpl implements InvitationService {
    private static final int CODE_BYTES = 16;
    private static final int MAX_ATTEMPTS = 8;
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private final SecureRandom random = new SecureRandom();
    private final InvitationMapper mapper;

    public InvitationServiceImpl(InvitationMapper mapper) { this.mapper = mapper; }

    @Override
    public String getOrCreateCode(Long memberId) {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String existing = mapper.findCode(memberId);
            if (existing != null) return existing;
            byte[] bytes = new byte[CODE_BYTES];
            random.nextBytes(bytes);
            String code = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            try {
                if (mapper.assignCode(memberId, code) == 1) return code;
            } catch (DuplicateKeyException collision) {
                // 唯一索引保证不同会员不会获得同一标识，碰撞时重新生成。
            }
        }
        throw new IllegalStateException("无法生成邀请标识");
    }

    @Override
    @Transactional
    public void bind(Long memberId, String code) {
        Long inviterId = mapper.findInviter(code);
        if (inviterId == null || inviterId.equals(memberId)) return;
        // 被邀请人唯一索引仲裁并发，重复请求不更新原邀请人。
        mapper.insertRelation(inviterId, memberId, LocalDateTime.now(BUSINESS_ZONE));
    }

    @Override
    public InvitationPageVO list(Long memberId, int page, int size) {
        var records = mapper.findPage(memberId, ((long) page - 1) * size, size).stream()
                .map(row -> new InvitationRecordVO(maskPhone(row.phone()), ApiTime.fromShanghai(row.createdAt())))
                .toList();
        return new InvitationPageVO(records, mapper.count(memberId), page, size);
    }

    private String maskPhone(String phone) {
        return phone != null && phone.matches("[0-9]{11}")
                ? phone.substring(0, 3) + "****" + phone.substring(7) : "****";
    }
}
