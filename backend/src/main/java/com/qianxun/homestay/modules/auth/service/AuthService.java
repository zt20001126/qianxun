package com.qianxun.homestay.modules.auth.service;

import com.qianxun.homestay.modules.auth.vo.AppLoginVO;
import com.qianxun.homestay.modules.auth.vo.MemberVO;

/**
 * 小程序会员认证业务入口。
 */
public interface AuthService {

    /**
     * 按手机号创建或复用会员并签发 30 天 JWT。
     *
     * @param phone 中国大陆手机号
     * @return 登录令牌及会员资料
     */
    AppLoginVO loginByPhone(String phone);

    /**
     * 根据 JWT 身份读取当前会员资料。
     *
     * @param memberId 会员 ID
     * @return 会员资料
     */
    MemberVO getCurrentMember(Long memberId);
}
