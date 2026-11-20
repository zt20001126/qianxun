package com.qianxun.homestay.modules.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.modules.auth.service.impl.WechatIdentityBindingServiceImpl;
import com.qianxun.homestay.modules.member.entity.MemberEntity;
import com.qianxun.homestay.modules.member.mapper.MemberMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;

/** 验证 OpenID 只能绑定到手机号匹配且未关联其他微信账号的会员。 */
class WechatIdentityBindingServiceImplTest {

    private static final Long MEMBER_ID = 10L;
    private static final String PHONE = "13800138000";
    private static final String OPENID = "wx-openid-001";

    private final MemberMapper memberMapper = mock(MemberMapper.class);
    private final WechatIdentityBindingServiceImpl service = new WechatIdentityBindingServiceImpl(memberMapper);
    private final MemberEntity member = new MemberEntity();

    @BeforeEach
    void prepareMember() {
        member.setId(MEMBER_ID);
        member.setPhone(PHONE);
        when(memberMapper.findByIdForUpdate(MEMBER_ID)).thenReturn(member);
        when(memberMapper.findByOpenid(OPENID)).thenReturn(null);
    }

    @Test
    void bindsVerifiedIdentityWhenPhoneMatches() {
        when(memberMapper.bindOpenidIfAbsent(MEMBER_ID, OPENID)).thenReturn(1);

        assertThat(service.bindVerifiedIdentity(MEMBER_ID, PHONE, OPENID)).isEqualTo(OPENID);

        verify(memberMapper).bindOpenidIfAbsent(MEMBER_ID, OPENID);
    }

    @Test
    void rejectsPhoneMismatchBeforeLookingUpOrWritingOpenId() {
        assertThatThrownBy(() -> service.bindVerifiedIdentity(MEMBER_ID, "13800138001", OPENID))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);

        verify(memberMapper, never()).findByOpenid(OPENID);
        verify(memberMapper, never()).bindOpenidIfAbsent(MEMBER_ID, OPENID);
    }

    @Test
    void rejectsOpenIdOwnedByAnotherMember() {
        MemberEntity owner = new MemberEntity();
        owner.setId(11L);
        when(memberMapper.findByOpenid(OPENID)).thenReturn(owner);

        assertThatThrownBy(() -> service.bindVerifiedIdentity(MEMBER_ID, PHONE, OPENID))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(memberMapper, never()).bindOpenidIfAbsent(MEMBER_ID, OPENID);
    }

    @Test
    void rejectsReplacingAnExistingDifferentOpenId() {
        member.setOpenid("wx-previous-openid");

        assertThatThrownBy(() -> service.bindVerifiedIdentity(MEMBER_ID, PHONE, OPENID))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(memberMapper, never()).bindOpenidIfAbsent(MEMBER_ID, OPENID);
    }

    @Test
    void existingSameBindingIsIdempotent() {
        member.setOpenid(OPENID);

        assertThat(service.bindVerifiedIdentity(MEMBER_ID, PHONE, OPENID)).isEqualTo(OPENID);

        verify(memberMapper, never()).bindOpenidIfAbsent(MEMBER_ID, OPENID);
    }

    @Test
    void mapsUniqueConstraintRaceToConflict() {
        when(memberMapper.bindOpenidIfAbsent(MEMBER_ID, OPENID))
                .thenThrow(new DuplicateKeyException("duplicate openid"));

        assertThatThrownBy(() -> service.bindVerifiedIdentity(MEMBER_ID, PHONE, OPENID))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void rejectsMissingMember() {
        when(memberMapper.findByIdForUpdate(MEMBER_ID)).thenReturn(null);

        assertThatThrownBy(() -> service.bindVerifiedIdentity(MEMBER_ID, PHONE, OPENID))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }
}
