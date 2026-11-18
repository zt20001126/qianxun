package com.qianxun.homestay.modules.member.mapper;

import com.qianxun.homestay.modules.member.entity.MemberEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 会员数据库访问入口，所有 SQL 由 member XML 映射文件维护。
 */
@Mapper
public interface MemberMapper {

    /**
     * 原子创建或复用会员，并同步第一阶段固定昵称与头像。
     *
     * @param phone 登录手机号
     * @param nickname 手机号后四位昵称
     * @param avatar 默认头像地址
     * @return 受影响行数
     */
    int upsertLoginProfile(@Param("phone") String phone, @Param("nickname") String nickname,
            @Param("avatar") String avatar);

    /**
     * 按登录手机号读取会员资料。
     *
     * @param phone 登录手机号
     * @return 会员实体；不存在时为空
     */
    MemberEntity findByPhone(@Param("phone") String phone);

    /**
     * 按 JWT 中的会员 ID 读取当前会员。
     *
     * @param id 会员 ID
     * @return 会员实体；不存在时为空
     */
    MemberEntity findById(@Param("id") Long id);
}
