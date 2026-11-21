package com.qianxun.homestay.modules.auth.service.impl;

import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.modules.auth.service.WechatIdentityBindingService;
import com.qianxun.homestay.modules.member.entity.MemberEntity;
import com.qianxun.homestay.modules.member.mapper.MemberMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

/** 事务化绑定已由微信验证的会员身份。 */
@Service
public class WechatIdentityBindingServiceImpl implements WechatIdentityBindingService {

    private final MemberMapper memberMapper;

    @Value("${app.auth.default-avatar}")
    private String defaultAvatar;

    public WechatIdentityBindingServiceImpl(MemberMapper memberMapper) {
        this.memberMapper = memberMapper;
    }

    @Override
    @Transactional
    public MemberEntity bindLoginIdentity(String phone, String openid) {
        // 唯一手机号约束串行化预建会员与新用户的并发创建，禁止改写已有积分。
        memberMapper.ensureExternalMember(phone, phone.substring(phone.length() - 4), defaultAvatar);
        MemberEntity member = memberMapper.findByPhoneForUpdate(phone);
        if (member == null) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "会员资料初始化失败");
        }
        bindVerifiedIdentity(member.getId(), phone, openid);
        return memberMapper.findById(member.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public String requireBoundOpenId(Long memberId, String openid) {
        MemberEntity member = memberMapper.findById(memberId);
        if (member == null || member.getOpenid() == null || !member.getOpenid().equals(openid)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "当前微信账号与会员绑定不一致，请联系管理员");
        }
        return member.getOpenid();
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
            throw new BusinessException(HttpStatus.CONFLICT, "该微信账号已绑定其他会员");
        }
        if (member.getOpenid() != null && !member.getOpenid().equals(openid)) {
            throw new BusinessException(HttpStatus.CONFLICT, "当前会员已绑定其他微信账号");
        }
        if (member.getOpenid() == null) {
            try {
                if (memberMapper.bindOpenidIfAbsent(memberId, openid) != 1) {
                    throw new BusinessException(HttpStatus.CONFLICT, "微信账号绑定状态已变化，请重试");
                }
            } catch (DuplicateKeyException exception) {
                throw new BusinessException(HttpStatus.CONFLICT, "该微信账号已绑定其他会员");
            }
        }
        return openid;
    }
}
