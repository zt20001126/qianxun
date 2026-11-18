package com.qianxun.homestay.modules.points.service;

import com.qianxun.homestay.modules.points.dto.PointsGrantRequest;
import com.qianxun.homestay.modules.points.enums.PointsLogType;
import com.qianxun.homestay.modules.points.vo.PointsGrantVO;
import com.qianxun.homestay.modules.points.vo.PointsLogPageVO;
import com.qianxun.homestay.modules.points.vo.PointsRewardRetryVO;
import com.qianxun.homestay.modules.points.vo.PointsRulesVO;

/** 提供积分规则、流水查询、人工发放与订单奖励重试业务。 */
public interface PointsManagementService {

    /** 查询积分等级、兑换比例、抵扣上限与有效期规则。 */
    PointsRulesVO getRules();

    /** 查询当前会员自己的积分流水。 */
    PointsLogPageVO findMyLogs(Long memberId, PointsLogType type, int page, int size);

    /** 按会员、订单和类型筛选后台积分流水。 */
    PointsLogPageVO findAdminLogs(Long memberId, Long orderId, PointsLogType type, int page, int size);

    /** 按会员行锁原子发放积分并写入幂等流水。 */
    PointsGrantVO grant(Long memberId, PointsGrantRequest request);

    /** 对符合条件的已完成订单安全重试一次性积分奖励。 */
    PointsRewardRetryVO retryOrderReward(Long orderId);
}
