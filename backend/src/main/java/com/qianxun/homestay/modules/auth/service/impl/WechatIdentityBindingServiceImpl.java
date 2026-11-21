package com.qianxun.homestay.modules.auth.service.impl;

import com.qianxun.homestay.common.exception.BusinessException;
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
