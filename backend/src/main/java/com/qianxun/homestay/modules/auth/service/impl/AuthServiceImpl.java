package com.qianxun.homestay.modules.auth.service.impl;

import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.common.security.service.JwtService;
import com.qianxun.homestay.modules.auth.service.AuthService;
import com.qianxun.homestay.modules.auth.vo.AppLoginVO;
import com.qianxun.homestay.modules.auth.vo.MemberVO;
import com.qianxun.homestay.modules.member.entity.MemberEntity;
import com.qianxun.homestay.modules.member.mapper.MemberMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 小程序手机号登录编排：固定资料、原子复用会员、读取会员并签发令牌。
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final MemberMapper memberMapper;
    private final JwtService jwtService;
    private final String defaultAvatar;

    public AuthServiceImpl(
            MemberMapper memberMapper,
            JwtService jwtService,
            @Value("${app.auth.default-avatar}") String defaultAvatar) {
        this.memberMapper = memberMapper;
        this.jwtService = jwtService;
        this.defaultAvatar = defaultAvatar;
    }

    /**
     * 手机号登录业务流程。
     *
     * <p>Step 1: 根据手机号末四位生成固定昵称。
     * Step 2: 通过唯一手机号索引和单条 upsert 原子创建或同步会员头像、昵称。
     * Step 3: 重新读取数据库会员并签发仅标识会员 ID 的 30 天 JWT。</p>
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
