/** 芊浔民宿微信手机号授权登录页：管理授权、提交状态并保存登录令牌。 */
import { login } from '../../services/auth'
import { getValidToken, saveSession } from '../../utils/session'
import { consumePendingInvitation } from '../../utils/invitation'

interface LoginPageData {
  loading: boolean
  errorMessage: string
  agreedTerms: boolean
}

Page({
  data: {
    loading: false,
    errorMessage: '',
    agreedTerms: false,
  } as LoginPageData,

  onShow() {
    if (getValidToken()) {
      void consumePendingInvitation()
      wx.switchTab({ url: '/pages/home/index' })
    }
  },

  onTermsToggle() {
    this.setData({ agreedTerms: !this.data.agreedTerms })
  },

  onAgreementTap(event: WechatMiniprogram.TouchEvent) {
    const type = event.currentTarget.dataset.type === 'privacy' ? 'privacy' : 'user'
    wx.navigateTo({ url: `/pages/agreement/index?type=${type}` })
  },

  async onPhoneAuthorized(event: { detail: { code?: string; errMsg?: string; errno?: number } }) {
    if (!this.data.agreedTerms) {
      wx.showToast({ title: '请先勾选同意用户协议与隐私协议', icon: 'none' })
      return
    }

    const phoneCode = event.detail.code
    if (!phoneCode) {
      const platformMessage = event.detail.errMsg || '微信没有返回授权结果'
      const errorNumber = event.detail.errno ? `（错误码 ${event.detail.errno}）` : ''
      const permissionIssue = /permission|not auth|unsupported|api.*disabled/i.test(platformMessage)
      const hint = permissionIssue
        ? '请在微信公众平台确认小程序已完成认证并开通手机号快速验证能力。'
        : '请确认已在微信授权弹窗中同意手机号授权后重试。'
      this.setData({ errorMessage: `手机号授权失败：${platformMessage}${errorNumber}。${hint}` })
      return
    }

    this.setData({ loading: true, errorMessage: '' })
    try {
      const loginCode = await new Promise<string>((resolve, reject) => wx.login({
        success: (result) => result.code ? resolve(result.code) : reject(new Error('获取微信登录凭证失败')),
        fail: () => reject(new Error('获取微信登录凭证失败，请重试')),
      }))
      const result = await login(loginCode, phoneCode)
      saveSession(result)
      void consumePendingInvitation()
      wx.switchTab({ url: '/pages/home/index' })
    } catch (error) {
      this.setData({ errorMessage: error instanceof Error ? error.message : '登录失败，请稍后重试' })
    } finally {
      this.setData({ loading: false })
    }
  },
})
