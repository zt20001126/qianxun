/** 统一管理 JWT、本地过期时间和会员会话。 */
import type { AppLoginResult, IAppOption, MemberProfile } from '../types/auth'

const TOKEN_KEY = 'qx_member_token'
const EXPIRES_AT_KEY = 'qx_member_token_expires_at'
const MEMBER_KEY = 'qx_member_profile'

export function saveSession(result: AppLoginResult): void {
  const expiresAt = Date.now() + result.expires_in * 1000
  wx.setStorageSync(TOKEN_KEY, result.token)
  wx.setStorageSync(EXPIRES_AT_KEY, expiresAt)
  wx.setStorageSync(MEMBER_KEY, result.member)
  const app = getApp<IAppOption>()
  app.globalData.token = result.token
  app.globalData.tokenExpiresAt = expiresAt
  app.globalData.member = result.member
}

export function getValidToken(): string {
  const token = wx.getStorageSync(TOKEN_KEY) as string
  const expiresAt = Number(wx.getStorageSync(EXPIRES_AT_KEY) || 0)
  if (!token || expiresAt <= Date.now()) {
    clearSession()
    return ''
  }
  const app = getApp<IAppOption>()
  app.globalData.token = token
  app.globalData.tokenExpiresAt = expiresAt
  return token
}

export function saveCurrentMember(member: MemberProfile): void {
  wx.setStorageSync(MEMBER_KEY, member)
  getApp<IAppOption>().globalData.member = member
}

export function getSavedMember(): MemberProfile | null {
  return (wx.getStorageSync(MEMBER_KEY) as MemberProfile | undefined) || null
}

export function clearSession(): void {
  wx.removeStorageSync(TOKEN_KEY)
  wx.removeStorageSync(EXPIRES_AT_KEY)
  wx.removeStorageSync(MEMBER_KEY)
  const app = getApp<IAppOption>()
  app.globalData.token = ''
  app.globalData.tokenExpiresAt = 0
  app.globalData.member = null
}
