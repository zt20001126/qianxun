import { defineStore } from 'pinia'
import { ref } from 'vue'
import { authApi } from '../api/auth'
import type { AdminProfile } from '../types/api'

/** 管理员会话状态：持久化 JWT 与公开资料，不保存管理员密码。 */
export const useAuthStore = defineStore('admin-auth', () => {
  const token = ref(localStorage.getItem('qx_admin_token') || '')
  const admin = ref<AdminProfile | null>(readProfile())

  function readProfile(): AdminProfile | null {
    const stored = localStorage.getItem('qx_admin_profile')
    if (!stored) return null
    try {
      return JSON.parse(stored) as AdminProfile
    } catch {
      localStorage.removeItem('qx_admin_profile')
      return null
    }
  }

  async function login(username: string, password: string): Promise<void> {
    const result = await authApi.login(username, password)
    token.value = result.token
    admin.value = result.admin
    localStorage.setItem('qx_admin_token', result.token)
    localStorage.setItem('qx_admin_profile', JSON.stringify(result.admin))
  }

  async function fetchMe(): Promise<AdminProfile> {
    const profile = await authApi.me()
    admin.value = profile
    localStorage.setItem('qx_admin_profile', JSON.stringify(profile))
    return profile
  }

  function clear(): void {
    token.value = ''
    admin.value = null
    localStorage.removeItem('qx_admin_token')
    localStorage.removeItem('qx_admin_profile')
  }

  return { token, admin, login, fetchMe, clear }
})
