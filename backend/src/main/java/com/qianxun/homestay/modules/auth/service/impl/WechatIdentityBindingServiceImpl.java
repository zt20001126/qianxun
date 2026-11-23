package com.qianxun.homestay.modules.auth.service.impl;

import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.modules.auth.domain.VerifiedWechatIdentity;
import com.qianxun.homestay.modules.auth.service.WechatIdentityBindingService;
import com.qianxun.homestay.modules.member.entity.MemberEntity;
import com.qianxun.homestay.modules.member.mapper.MemberMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 事务化绑定已由微信验证的会员身份。 */
@Service
public class WechatIdentityBindingServiceImpl implements WechatIdentityBindingService {

    private final MemberMapper memberMapper;

    public WechatIdentityBindingServiceImpl(MemberMapper memberMapper) {
        this.memberMapper = memberMapper;
    }

    /** 登录时复用同手机号会员，仅填充空 OpenID，避免更改其历史业务数据。 */
    @Override
    @Transactional
    public Long loginOrBindVerifiedIdentity(VerifiedWechatIdentity identity, String defaultAvatar) {
        String phone = identity.phone();
        String openid = identity.openid();

        // 创建/更新仅按手机号命中的固定资料，SQL 不带 OpenID，避免 OpenID 唯一冲突时误选其他会员。
        memberMapper.upsertLoginProfile(phone, phone.substring(phone.length() - 4), defaultAvatar);
        MemberEntity member = memberMapper.findByPhoneForUpdate(phone);
        if (member == null) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "会员资料初始化失败");
        }

        MemberEntity owner = memberMapper.findByOpenid(openid);
        if (owner != null && !member.getId().equals(owner.getId())) {
            throw bindingConflict();
        }
        if (member.getOpenid() != null && !member.getOpenid().equals(openid)) {
            throw bindingConflict();
        }
        if (member.getOpenid() == null) {
            try {
                if (memberMapper.bindOpenidIfAbsent(member.getId(), openid) != 1) {
                    throw bindingConflict();
                }
            } catch (DuplicateKeyException exception) {
                throw bindingConflict();
            }
        }
        return member.getId();
    }

    @Override
    @Transactional
    public String bindVerifiedIdentity(Long memberId, String phone, String openid) {
        MemberEntity member = memberMapper.findByIdForUpdate(memberId);
        if (member == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "会员不存在");
        }
        if (!phone.equals(member.getPhone())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "微信授权手机号与当前会员手机号不一致");
        }

        MemberEntity owner = memberMapper.findByOpenid(openid);
        if (owner != null && !memberId.equals(owner.getId())) {
            throw bindingConflict();
        }
        if (member.getOpenid() != null && !member.getOpenid().equals(openid)) {
            throw bindingConflict();
        }
        if (member.getOpenid() == null) {
            try {
                if (memberMapper.bindOpenidIfAbsent(memberId, openid) != 1) {
                    throw bindingConflict();
                }
            } catch (DuplicateKeyException exception) {
                throw bindingConflict();
            }
        }
        return openid;
    }

    private BusinessException bindingConflict() {
        return new BusinessException(HttpStatus.CONFLICT, "账号绑定信息存在冲突，请联系管理员处理");
    }
}
