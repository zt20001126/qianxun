/** 芊浔民宿手机号登录页：输入校验、提交状态、登录后保存 JWT。 */
import { login } from '../../services/auth'
import { isValidMainlandPhone } from '../../utils/phone'
import { getValidToken, saveSession } from '../../utils/session'

interface LoginPageData {
  phone: string
  loading: boolean
  errorMessage: string
  agreedTerms: boolean
}

Page({
  data: {
    phone: '',
    loading: false,
    errorMessage: '',
    agreedTerms: false,
  } as LoginPageData,

  onShow() {
    if (getValidToken()) {
      wx.switchTab({ url: '/pages/home/index' })
    }
  },

  onPhoneInput(event: WechatMiniprogram.Input) {
    const phone = event.detail.value.replace(/\D/g, '').slice(0, 11)
    this.setData({ phone, errorMessage: '' })
  },

  onTermsToggle() {
    this.setData({ agreedTerms: !this.data.agreedTerms })
  },

  async onLoginTap() {
    if (!this.data.agreedTerms) {
      wx.showToast({ title: '请先勾选同意用户协议与隐私协议', icon: 'none' })
      return
    }

    const phone = this.data.phone.trim()
    if (!isValidMainlandPhone(phone)) {
      this.setData({ errorMessage: '请输入有效的 11 位中国大陆手机号' })
      return
    }

    this.setData({ loading: true, errorMessage: '' })
    try {
      const result = await login(phone)
      saveSession(result)
      wx.switchTab({ url: '/pages/home/index' })
    } catch (error) {
      this.setData({ errorMessage: error instanceof Error ? error.message : '登录失败，请稍后重试' })
    } finally {
      this.setData({ loading: false })
    }
  },
})
