/** 「我的」页面：展示登录资料并提供常用页面入口。 */
import { DEFAULT_AVATAR_PATH } from '../../config'
import { callBusinessPhone } from '../../utils/business-contact'
import { getSavedMember, getValidToken } from '../../utils/session'

interface MyPageData {
  avatarSource: string
  nickname: string
  profileHint: string
}

Page({
  data: {
    avatarSource: DEFAULT_AVATAR_PATH,
    nickname: '芊浔游客',
    profileHint: '欢迎来到芊浔民宿',
  } as MyPageData,

  onLoad() {
    wx.setNavigationBarTitle({ title: '我的' })
  },

  onShow() {
    const isLoggedIn = Boolean(getValidToken())
    const member = isLoggedIn ? getSavedMember() : null
    this.setData({
      avatarSource: member?.avatar || DEFAULT_AVATAR_PATH,
      nickname: member?.nickname || '芊浔游客',
      profileHint: member ? '欢迎回来' : '欢迎来到芊浔民宿',
    })
  },

  onOrdersTap() {
    wx.switchTab({ url: '/pages/orders/index' })
  },

  onMemberCenterTap() {
    wx.switchTab({ url: '/pages/member-center/index' })
  },

  onContactTap() {
    // 复用现有的商家联系电话能力；当前小程序没有独立的联系我们页面。
    callBusinessPhone()
  },
})
