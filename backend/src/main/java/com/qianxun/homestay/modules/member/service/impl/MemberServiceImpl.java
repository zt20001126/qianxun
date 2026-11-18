package com.qianxun.homestay.modules.member.service.impl;

import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.common.util.ApiTime;
import com.qianxun.homestay.modules.member.domain.MemberLevelCalculator;
import com.qianxun.homestay.modules.member.dto.MemberCreateRequest;
import com.qianxun.homestay.modules.member.entity.MemberEntity;
import com.qianxun.homestay.modules.member.enums.MemberLevel;
import com.qianxun.homestay.modules.member.mapper.MemberMapper;
import com.qianxun.homestay.modules.member.service.MemberService;
import com.qianxun.homestay.modules.member.vo.AppMemberVO;
import com.qianxun.homestay.modules.member.vo.MemberCreatedVO;
import com.qianxun.homestay.modules.member.vo.MemberListItemVO;
import com.qianxun.homestay.modules.member.vo.MemberPageVO;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 编排会员查询、等级计算与后台预建会员流程。 */
@Service
public class MemberServiceImpl implements MemberService {

    private final MemberMapper memberMapper;
    private final MemberLevelCalculator levelCalculator;
    private final String defaultAvatar;

    public MemberServiceImpl(
            MemberMapper memberMapper,
            MemberLevelCalculator levelCalculator,
            @Value("${app.auth.default-avatar}") String defaultAvatar) {
        this.memberMapper = memberMapper;
        this.levelCalculator = levelCalculator;
        this.defaultAvatar = defaultAvatar;
    }

    @Override
    @Transactional(readOnly = true)
    public AppMemberVO getMyMember(Long memberId) {
        MemberEntity member = memberMapper.findById(memberId);
        if (member == null) throw new BusinessException(HttpStatus.NOT_FOUND, "会员不存在");
        MemberLevel level = levelCalculator.levelFor(member.getGrowthPoints());
        return new AppMemberVO(member.getId(), maskPhone(member.getPhone()), member.getNickname(), member.getAvatar(),
                member.getGrowthPoints(), member.getPoints(), level.getDisplayName(),
                levelCalculator.pointsToNextLevel(member.getGrowthPoints()),
                levelCalculator.progressPercent(member.getGrowthPoints()));
    }

    @Override
    @Transactional(readOnly = true)
    public MemberPageVO findMembers(String phone, int page, int size) {
        String phoneFilter = phone == null || phone.isBlank() ? null : phone.trim();
        long offset = (long) (page - 1) * size;
        List<MemberListItemVO> list = memberMapper.findPage(phoneFilter, offset, size).stream()
                .map(this::toListItem).toList();
        return new MemberPageVO(list, memberMapper.count(phoneFilter), page, size);
    }

    @Override
    @Transactional
    public MemberCreatedVO createMember(MemberCreateRequest request) {
        String phone = request.phone().trim();
        if (memberMapper.findByPhone(phone) != null) {
            throw new BusinessException(HttpStatus.CONFLICT, "该手机号已存在会员");
        }
        MemberEntity member = new MemberEntity();
        member.setPhone(phone);
        member.setNickname(phone.substring(phone.length() - 4));
        member.setAvatar(defaultAvatar);
        member.setGrowthPoints(0);
        member.setPoints(0);
        try {
            memberMapper.insertMember(member);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(HttpStatus.CONFLICT, "该手机号已存在会员");
        }
        return new MemberCreatedVO(member.getId(), member.getPhone());
    }

    private MemberListItemVO toListItem(MemberEntity member) {
        return new MemberListItemVO(member.getId(), member.getPhone(), member.getNickname(), member.getGrowthPoints(),
                member.getPoints(), levelCalculator.levelFor(member.getGrowthPoints()).getDisplayName(),
                ApiTime.fromShanghai(member.getCreatedAt()));
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return "****";
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
