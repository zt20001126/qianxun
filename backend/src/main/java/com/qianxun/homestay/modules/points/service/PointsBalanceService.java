package com.qianxun.homestay.modules.points.service;

import com.qianxun.homestay.modules.member.entity.MemberEntity;
import com.qianxun.homestay.modules.points.domain.PointsRedemptionQuote;
import com.qianxun.homestay.modules.points.dto.PointsPreviewRequest;
import com.qianxun.homestay.modules.points.vo.PointsPreviewVO;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 小程序积分可用余额、抵扣试算及支付扣减业务。 */
public interface PointsBalanceService {

    /** 查询当前会员在扣除其他有效锁房预占后的积分抵扣试算。 */
    PointsPreviewVO preview(Long memberId, PointsPreviewRequest request);

    /** 在会员行已锁定的事务中计算可用余额并校验本次预占积分。 */
    PointsRedemptionQuote calculateForLockedMember(
            MemberEntity member, BigDecimal roomAmount, Integer pointsUsed, LocalDateTime now);

    /** 模拟支付成功时扣减积分并写入带余额快照的抵扣流水。 */
    void consumeForPaidOrder(
            Long memberId, int pointsUsed, BigDecimal discountAmount, Long orderId);
}
