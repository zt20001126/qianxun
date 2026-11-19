/** 「我的」页面：展示会员名片、商家电话和退出登录操作。 */
import { getMyMember } from '../../services/member'
import type { AppMember } from '../../types/member'
import { STORE_CONTACT_PHONE_LABEL } from '../../config'
import { callBusinessPhone } from '../../utils/business-contact'
import { clearSession, getSavedMember, getValidToken } from '../../utils/session'

interface MyPageData {
  nickname: string
  maskedPhone: string
  levelLabel: string
  contactPhone: string
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

function toPageData(member: Pick<AppMember, 'phone' | 'level'>): MyPageData {
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
  } as MyPageData,

  onLoad() {
    wx.setNavigationBarTitle({ title: '我的' })
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

  onLogoutTap() {
    clearSession()
    wx.reLaunch({ url: '/pages/login/index' })
  },
})
