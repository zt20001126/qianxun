import axios from 'axios'
import type { ApiResponse } from '../types/api'

/** 管理后台统一 Axios 实例，集中维护 API 地址、令牌注入和失效处理。 */
export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  timeout: 10_000,
  headers: { 'Content-Type': 'application/json' },
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('qx_admin_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

http.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResponse<unknown>
    if (body.code !== 200) return Promise.reject(new Error(body.message || '请求失败'))
    return response
  },
  (error: unknown) => {
    if (axios.isAxiosError<ApiResponse<unknown>>(error) && error.response?.status === 401) {
      localStorage.removeItem('qx_admin_token')
      localStorage.removeItem('qx_admin_profile')
      if (window.location.pathname !== '/login') window.location.assign('/login')
    }
    const message = axios.isAxiosError<ApiResponse<unknown>>(error)
      ? error.response?.data?.message || error.message
      : error instanceof Error ? error.message : '请求失败'
    return Promise.reject(new Error(message))
  },
)
