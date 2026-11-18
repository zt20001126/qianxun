/** 认证成功占位页：调用 A02 获取服务端当前会员资料。 */
import { DEFAULT_AVATAR_PATH } from '../../config'
import { getCurrentMember } from '../../services/auth'
import type { MemberProfile } from '../../types/auth'
import { clearSession, saveCurrentMember } from '../../utils/session'

interface HomePageData {
  member: MemberProfile | null
  avatarSource: string
  loading: boolean
  errorMessage: string
}

Page({
  data: {
    member: null,
    avatarSource: '../../assets/avatar-default.webp',
    loading: true,
    errorMessage: '',
  } as HomePageData,

  async onLoad() {
    await this.loadMember()
  },

  async loadMember() {
    this.setData({ loading: true, errorMessage: '' })
    try {
      const member = await getCurrentMember()
      saveCurrentMember(member)
      this.setData({
        member,
        avatarSource: member.avatar === DEFAULT_AVATAR_PATH ? '../../assets/avatar-default.webp' : member.avatar,
      })
    } catch (error) {
      this.setData({ errorMessage: error instanceof Error ? error.message : '会员信息读取失败' })
    } finally {
      this.setData({ loading: false })
    }
  },

  onLogoutTap() {
    clearSession()
    wx.reLaunch({ url: '/pages/login/index' })
  },
})
