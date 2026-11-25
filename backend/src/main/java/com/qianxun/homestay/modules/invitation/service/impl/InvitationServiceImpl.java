package com.qianxun.homestay.modules.invitation.service.impl;

import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.common.util.ApiTime;
import com.qianxun.homestay.modules.invitation.mapper.InvitationMapper;
import com.qianxun.homestay.modules.invitation.enums.InvitationRewardFilter;
import com.qianxun.homestay.modules.invitation.service.InvitationService;
import com.qianxun.homestay.modules.invitation.vo.AdminInvitationPageVO;
import com.qianxun.homestay.modules.invitation.vo.AdminInvitationRecordVO;
import com.qianxun.homestay.modules.invitation.vo.InvitationPageVO;
import com.qianxun.homestay.modules.invitation.vo.InvitationRecordVO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 在数据库约束保护下生成邀请标识并建立不可自动更换的关系。 */
@Service
public class InvitationServiceImpl implements InvitationService {
    private static final int CODE_BYTES = 16;
    private static final int MAX_ATTEMPTS = 8;
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final BigDecimal REWARD_RATE = new BigDecimal("0.10");
    private final LocalDateTime effectiveAt;
    private final SecureRandom random = new SecureRandom();
    private final InvitationMapper mapper;

    public InvitationServiceImpl(InvitationMapper mapper) {
        this.mapper = mapper;
        // 使用数据库迁移时间，所有实例及重启后共享同一生效边界。
        this.effectiveAt = mapper.findRewardEffectiveAt();
        if (this.effectiveAt == null) {
            throw new IllegalStateException("邀请奖励生效迁移记录不存在");
        }
    }

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
                .map(row -> new InvitationRecordVO(maskPhone(row.phone()), ApiTime.fromShanghai(row.createdAt()), row.hasCompletedOrder()))
                .toList();
        return new InvitationPageVO(records, mapper.count(memberId), page, size);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void lockRewardMember(Long memberId) {
        if (memberId != null) {
            mapper.lockMember(memberId);
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordReward(Long memberId, Long orderId) {
        if (memberId == null) {
            return;
        }
        // 对同一被邀请人加行锁，避免不同客房并发退房重复计算首单。
        Long relationId = mapper.lockRelation(memberId);
        if (relationId == null) {
            return;
        }
        // 普通一致性读取只访问当前订单，避免多表 UPDATE 扩大订单锁范围。
        BigDecimal amount = mapper.findEligibleAmount(relationId, orderId, effectiveAt);
        if (amount != null) {
            BigDecimal reward = amount.multiply(REWARD_RATE).setScale(2, RoundingMode.HALF_UP);
            mapper.recordReward(relationId, orderId, reward);
        }
    }

    @Override
    public AdminInvitationPageVO adminList(int page, int size, String phone, InvitationRewardFilter rewardStatus) {
        var records = mapper.findAdminPage(((long) page - 1) * size, size, phone, rewardStatus).stream()
                .map(row -> new AdminInvitationRecordVO(row.id(), row.inviterId(), row.inviterPhone(),
                        row.inviteeId(), row.inviteePhone(), ApiTime.fromShanghai(row.boundAt()), row.orderNo(),
                        row.paidAmount() == null ? null : row.paidAmount().toPlainString(),
                        row.rewardAmount() == null ? null : row.rewardAmount().toPlainString(), row.rewardIssued()))
                .toList();
        return new AdminInvitationPageVO(records, mapper.countAll(phone, rewardStatus), page, size);
    }

    @Override
    @Transactional
    public void setIssued(Long id, boolean issued) {
        Integer hasReward = mapper.hasReward(id);
        if (hasReward == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "邀请记录不存在");
        }
        if (hasReward == 0) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "该邀请尚未产生奖励");
        }
        if (!issued) {
            // 发放一经登记不可撤销，后端同样拒绝绕过页面的修改请求。
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "奖励发放状态不可改回未发放");
        }
        mapper.setIssued(id);
    }

    private String maskPhone(String phone) {
        return phone != null && phone.matches("[0-9]{11}")
                ? phone.substring(0, 3) + "****" + phone.substring(7) : "****";
    }
}
