/** 芊浔民宿微信手机号授权登录页。 */
import { login } from '../../services/auth'
import { getValidToken, saveSession } from '../../utils/session'

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
      wx.switchTab({ url: '/pages/home/index' })
    }
  },

  onTermsToggle() {
    this.setData({ agreedTerms: !this.data.agreedTerms })
  },

  async onPhoneAuthorized(event: { detail: { code?: string; errMsg?: string } }) {
    if (!this.data.agreedTerms) {
      wx.showToast({ title: '请先勾选同意用户协议与隐私协议', icon: 'none' })
      return
    }

    const phoneCode = event.detail.code
    if (!phoneCode) {
      // 仅记录微信返回的状态信息，不记录一次性授权凭证。
      console.warn('手机号授权未完成:', event.detail.errMsg || '微信未返回状态信息')
      this.setData({ errorMessage: '手机号授权未完成，请重试' })
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
      wx.switchTab({ url: '/pages/home/index' })
    } catch (error) {
      this.setData({ errorMessage: error instanceof Error ? error.message : '登录失败，请稍后重试' })
    } finally {
      this.setData({ loading: false })
    }
  },
})
