package com.qianxun.homestay.modules.auth.service.impl;

import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.common.security.service.JwtService;
import com.qianxun.homestay.modules.auth.service.AuthService;
import com.qianxun.homestay.modules.auth.service.WechatIdentityBindingService;
import com.qianxun.homestay.modules.auth.service.WechatIdentityService;
import com.qianxun.homestay.modules.auth.vo.AppLoginVO;
import com.qianxun.homestay.modules.auth.vo.MemberVO;
import com.qianxun.homestay.modules.member.entity.MemberEntity;
import com.qianxun.homestay.modules.member.mapper.MemberMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 小程序微信授权登录编排：验证身份、绑定会员并签发令牌。
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final MemberMapper memberMapper;
    private final JwtService jwtService;
    private final WechatIdentityService wechatIdentityService;
    private final WechatIdentityBindingService bindingService;
    private final String defaultAvatar;

    public AuthServiceImpl(
            MemberMapper memberMapper,
            JwtService jwtService,
            WechatIdentityService wechatIdentityService,
            WechatIdentityBindingService bindingService,
            @Value("${app.auth.default-avatar}") String defaultAvatar) {
        this.memberMapper = memberMapper;
        this.jwtService = jwtService;
        this.wechatIdentityService = wechatIdentityService;
        this.bindingService = bindingService;
        this.defaultAvatar = defaultAvatar;
    }

    /**
     * 微信授权登录流程：先验证微信凭证，再在独立事务中绑定并签发令牌。
     */
    @Override
    public AppLoginVO loginWithWechat(String loginCode, String phoneCode) {
        var identity = wechatIdentityService.verifyLoginCredentials(loginCode, phoneCode);
        Long memberId = bindingService.loginOrBindVerifiedIdentity(identity, defaultAvatar);
        MemberEntity member = memberMapper.findById(memberId);
        if (member == null) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "会员资料初始化失败");
        }
        return new AppLoginVO(jwtService.issueMemberToken(member.getId()),
                jwtService.getMemberExpirationSeconds(), toMemberVO(member));
    }

    /**
     * 直接按手机号复用会员资料；该登录方式不验证号码归属。
     */
    @Override
    @Transactional
    public AppLoginVO loginByPhone(String phone) {
        String nickname = phone.substring(phone.length() - 4);
        memberMapper.upsertLoginProfile(phone, nickname, defaultAvatar);
        MemberEntity member = memberMapper.findByPhone(phone);
        if (member == null) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "会员资料初始化失败");
        }
        return new AppLoginVO(jwtService.issueMemberToken(member.getId()),
                jwtService.getMemberExpirationSeconds(), toMemberVO(member));
    }

    /**
     * 读取当前会员流程。
     *
     * 仅使用 JWT 中的会员 ID 查询会员；不存在时返回 404。
     */
    @Override
    @Transactional(readOnly = true)
    public MemberVO getCurrentMember(Long memberId) {
        MemberEntity member = memberMapper.findById(memberId);
        if (member == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "会员不存在");
        }
        return toMemberVO(member);
    }

    private MemberVO toMemberVO(MemberEntity member) {
        return new MemberVO(member.getId(), member.getPhone(), member.getNickname(), member.getAvatar(),
                member.getPoints(), member.getGrowthPoints());
    }
}
