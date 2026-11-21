package com.qianxun.homestay.modules.auth.service.impl;

import com.qianxun.homestay.common.security.service.JwtService;
import com.qianxun.homestay.modules.auth.service.AuthService;
import com.qianxun.homestay.modules.auth.service.WechatIdentityService;
import com.qianxun.homestay.modules.auth.service.WechatIdentityBindingService;
import com.qianxun.homestay.modules.auth.vo.AppLoginVO;
import com.qianxun.homestay.modules.auth.vo.MemberVO;
import com.qianxun.homestay.modules.member.entity.MemberEntity;
import com.qianxun.homestay.modules.member.mapper.MemberMapper;
import com.qianxun.homestay.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 小程序微信授权登录编排：验证微信身份、复用会员并签发令牌。
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final MemberMapper memberMapper;
    private final JwtService jwtService;
    private final WechatIdentityService identityService;
    private final WechatIdentityBindingService bindingService;

    public AuthServiceImpl(
            MemberMapper memberMapper,
            JwtService jwtService,
            WechatIdentityService identityService,
            WechatIdentityBindingService bindingService) {
        this.memberMapper = memberMapper;
        this.jwtService = jwtService;
        this.identityService = identityService;
        this.bindingService = bindingService;
    }

    /**
     * 微信验证与会员绑定成功后签发令牌，不以用户输入手机号作为身份证明。
     */
    @Override
    public AppLoginVO loginWithWechat(String loginCode, String phoneCode) {
        String openid = identityService.resolveOpenId(loginCode);
        String phone = identityService.resolvePhone(phoneCode);
        MemberEntity member = bindingService.bindLoginIdentity(phone, openid);
        return new AppLoginVO(jwtService.issueMemberToken(member.getId()),
                jwtService.getMemberExpirationSeconds(), toMemberVO(member));
    }

    /**
     * 读取当前会员流程。
     *
     * <p>Step 1: 仅使用 JWT 中的会员 ID 查询会员。
     * Step 2: 未找到时返回 404，不使用手机号推断身份。</p>
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
