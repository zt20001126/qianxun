/** 邀请入口参数独立暂存；请求失败时不阻碍登录与浏览。 */
import { API_BASE_URL } from '../config'
import { getValidToken } from './session'

const KEY = 'qx_pending_invitation'
let running = false

export function savePendingInvitation(code: string): void {
  if (/^[A-Za-z0-9_-]{22}$/.test(code)) wx.setStorageSync(KEY, code)
}

export function clearPendingInvitation(): void {
  wx.removeStorageSync(KEY)
}

export async function consumePendingInvitation(): Promise<void> {
  if (running) return
  const code = wx.getStorageSync(KEY) as string
  if (!code) return
  const token = getValidToken()
  if (!token) return
  running = true
  try {
    const status = await new Promise<number>((resolve, reject) => {
      wx.request({
        url: `${API_BASE_URL}/api/app/invitations/bind`, method: 'POST',
        header: { 'content-type': 'application/json', Authorization: `Bearer ${token}` },
        data: { invite_code: code },
        success: response => resolve(response.statusCode), fail: reject,
      })
    })
    if ((status === 200 || status === 400) && wx.getStorageSync(KEY) === code && getValidToken() === token) {
      clearPendingInvitation()
    }
  } catch {
    // 网络异常保留参数，等待下次进入首页或登录后重试。
  } finally {
    running = false
    const next = wx.getStorageSync(KEY) as string
    if (next && next !== code) void consumePendingInvitation()
  }
}
