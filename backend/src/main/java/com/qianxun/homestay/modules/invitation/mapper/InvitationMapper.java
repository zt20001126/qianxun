package com.qianxun.homestay.modules.invitation.mapper;

import com.qianxun.homestay.modules.invitation.entity.InvitationRecordEntity;
import com.qianxun.homestay.modules.invitation.enums.InvitationRewardFilter;
import com.qianxun.homestay.modules.invitation.entity.AdminInvitationRecordEntity;
import java.math.BigDecimal;
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
    /** 提前锁定会员，统一退房积分和邀请奖励锁顺序。 */
    Long lockMember(@Param("memberId") Long memberId);
    /** 查询版本迁移完成时的北京时间，作为固定生效边界。 */
    LocalDateTime findRewardEffectiveAt();
    /** 锁定被邀请人的关系，串行化首单奖励。 */
    Long lockRelation(@Param("memberId") Long memberId);
    /** 查询满足绑定与生效时间边界的已完成订单实付金额。 */
    BigDecimal findEligibleAmount(@Param("relationId") Long relationId, @Param("orderId") Long orderId,
                                  @Param("effectiveAt") LocalDateTime effectiveAt);
    /** 原子保存首单及金额。 */
    int recordReward(@Param("relationId") Long relationId, @Param("orderId") Long orderId,
                     @Param("rewardAmount") BigDecimal rewardAmount);
    /** 查询后台邀请总数。 */
    long countAll(@Param("phone") String phone, @Param("rewardStatus") InvitationRewardFilter rewardStatus);
    /** 查询后台邀请及首单奖励。 */
    List<AdminInvitationRecordEntity> findAdminPage(@Param("offset") long offset, @Param("size") int size,
            @Param("phone") String phone, @Param("rewardStatus") InvitationRewardFilter rewardStatus);
    /** 查询关系是否存在以及是否有奖励。 */
    Integer hasReward(@Param("id") Long id);
    /** 人工登记奖励发放状态。 */
    int setIssued(@Param("id") Long id);
}
