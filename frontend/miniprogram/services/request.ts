/** 全局 wx.request 封装：注入会员 JWT、解包统一响应并处理 401。 */
import { API_BASE_URL } from '../config'
import type { ApiResponse } from '../types/auth'
import { clearSession, getValidToken } from '../utils/session'

interface RequestOptions {
  url: string
  method?: 'GET' | 'POST'
  data?: Record<string, unknown>
  withAuth?: boolean
}

let redirectingToLogin = false

export function request<T>(options: RequestOptions): Promise<T> {
  const withAuth = options.withAuth !== false
  const token = withAuth ? getValidToken() : ''
  if (withAuth && !token) {
    redirectToLogin()
    return Promise.reject(new Error('登录状态已失效，请重新登录'))
  }

  return new Promise<T>((resolve, reject) => {
    wx.request<ApiResponse<T>>({
      url: `${API_BASE_URL}${options.url}`,
      method: options.method || 'GET',
      data: options.data,
      header: {
        'content-type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      success: (response) => {
        if (response.statusCode === 401 && token) {
          // 只有当前请求使用的令牌仍生效时才退出，避免旧响应清除新登录会话。
          if (getValidToken() === token) {
            clearSession()
            redirectToLogin()
          }
          reject(new Error('登录状态已失效，请重新登录'))
          return
        }
        const body = response.data as ApiResponse<T>
        if (response.statusCode < 200 || response.statusCode >= 300 || body?.code !== 200) {
          reject(new Error(body?.message || '请求失败，请稍后重试'))
          return
        }
        resolve(body.data)
      },
      fail: () => reject(new Error('网络连接失败，请检查网络后重试')),
    })
  })
}

function redirectToLogin(): void {
  if (redirectingToLogin) return
  redirectingToLogin = true
  wx.reLaunch({
    url: '/pages/login/index',
    complete: () => { redirectingToLogin = false },
  })
}
