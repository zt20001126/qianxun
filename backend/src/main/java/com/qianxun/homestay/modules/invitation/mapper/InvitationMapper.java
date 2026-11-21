package com.qianxun.homestay.modules.invitation.mapper;

import com.qianxun.homestay.modules.invitation.entity.InvitationRecordEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 邀请标识与邀请关系的数据访问。 */
@Mapper
public interface InvitationMapper {
    String findCode(@Param("memberId") Long memberId);
    int assignCode(@Param("memberId") Long memberId, @Param("code") String code);
    Long findInviter(@Param("code") String code);
    int insertRelation(@Param("inviterId") Long inviterId, @Param("inviteeId") Long inviteeId,
                       @Param("now") LocalDateTime now);
    long count(@Param("inviterId") Long inviterId);
    List<InvitationRecordEntity> findPage(@Param("inviterId") Long inviterId,
                                          @Param("offset") long offset, @Param("size") int size);
}
