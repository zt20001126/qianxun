/** 「我的」页面：展示会员名片、商家电话和退出登录操作。 */
import { getMyMember } from '../../services/member'
import type { AppMember } from '../../types/member'
import { STORE_CONTACT_PHONE_LABEL } from '../../config'
import { callBusinessPhone } from '../../utils/business-contact'
import { clearSession, getSavedMember, getValidToken } from '../../utils/session'
import { clearPendingInvitation } from '../../utils/invitation'

interface MyPageData {
  nickname: string
  maskedPhone: string
  levelLabel: string
  contactPhone: string
  statusBarHeight: number
  navigationHeight: number
}

function getPhoneSuffix(phone: string): string {
  const digits = phone.replace(/\D/g, '')
  return digits.slice(-4) || '----'
}

function maskPhone(phone: string): string {
  if (/^\d{3}\*{4}\d{4}$/.test(phone)) return phone
  const digits = phone.replace(/\D/g, '')
  return digits.length === 11 ? `${digits.slice(0, 3)}****${digits.slice(-4)}` : phone
}

function formatLevelLabel(level: string): string {
  const labelMap: Record<string, string> = {
    普通: '普通会员',
    银: '银卡会员',
    银卡: '银卡会员',
    金: '金卡会员',
    金卡: '金卡会员',
    钻石: '钻石会员',
  }
  return labelMap[level] || (level.endsWith('会员') ? level : `${level}会员`)
}

function formatCachedLevel(growthPoints: number): string {
  if (growthPoints >= 1500) return '钻石会员'
  if (growthPoints >= 1000) return '金卡会员'
  if (growthPoints >= 500) return '银卡会员'
  return '普通会员'
}

function toPageData(member: Pick<AppMember, 'phone' | 'level'>): Pick<MyPageData, 'nickname' | 'maskedPhone' | 'levelLabel' | 'contactPhone'> {
  return {
    nickname: `尾号 ${getPhoneSuffix(member.phone)}`,
    maskedPhone: maskPhone(member.phone),
    levelLabel: formatLevelLabel(member.level),
    contactPhone: STORE_CONTACT_PHONE_LABEL,
  }
}

Page({
  data: {
    nickname: '',
    maskedPhone: '',
    levelLabel: '',
    contactPhone: STORE_CONTACT_PHONE_LABEL,
    statusBarHeight: 0,
    navigationHeight: 44,
  } as MyPageData,

  onLoad() {
    // 以系统状态栏和胶囊位置计算导航高度，避免遮挡原生控件。
    const system = wx.getSystemInfoSync()
    const statusBarHeight = system.statusBarHeight || 0
    const capsule = wx.getMenuButtonBoundingClientRect()
    const navigationHeight = capsule.height ? (capsule.top - statusBarHeight) * 2 + capsule.height : 44
    this.setData({ statusBarHeight, navigationHeight })
  },

  onShow() {
    if (!getValidToken()) {
      wx.reLaunch({ url: '/pages/login/index' })
      return
    }
    void this.loadMemberProfile()
  },

  async loadMemberProfile() {
    try {
      const member = await getMyMember()
      this.setData(toPageData(member))
    } catch {
      const savedMember = getSavedMember()
      if (!savedMember) {
        clearSession()
        wx.reLaunch({ url: '/pages/login/index' })
        return
      }
      this.setData({
        nickname: `尾号 ${getPhoneSuffix(savedMember.phone)}`,
        maskedPhone: maskPhone(savedMember.phone),
        levelLabel: formatCachedLevel(savedMember.growth_points),
      })
    }
  },

  onProfileTap() {
    wx.switchTab({ url: '/pages/member-center/index' })
  },

  onContactTap() {
    callBusinessPhone()
  },

  onInvitationTap() {
    wx.navigateTo({ url: '/pages/invitation/index' })
  },

  onRoomOrdersTap() {
    wx.navigateTo({ url: '/pages/orders/index' })
  },

  onMallOrdersTap() {
    wx.navigateTo({ url: '/pages/mall-orders/index' })
  },

  onLogoutTap() {
    clearPendingInvitation()
    clearSession()
    wx.reLaunch({ url: '/pages/login/index' })
  },
})
