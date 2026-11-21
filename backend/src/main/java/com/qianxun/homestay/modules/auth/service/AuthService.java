package com.qianxun.homestay.modules.auth.service;

import com.qianxun.homestay.modules.auth.vo.AppLoginVO;
import com.qianxun.homestay.modules.auth.vo.MemberVO;

/**
 * 小程序会员认证业务入口。
 */
public interface AuthService {

    /**
     * 验证微信身份并创建或复用会员，签发 30 天 JWT。
     *
     * @param loginCode 微信登录凭证
     * @param phoneCode 微信手机号授权凭证
     * @return 登录令牌及会员资料
     */
    AppLoginVO loginWithWechat(String loginCode, String phoneCode);

    /**
     * 根据 JWT 身份读取当前会员资料。
     *
     * @param memberId 会员 ID
     * @return 会员资料
     */
    MemberVO getCurrentMember(Long memberId);
}
