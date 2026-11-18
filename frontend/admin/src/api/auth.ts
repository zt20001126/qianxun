import { http } from './http'
import type { AdminLoginResult, AdminProfile, ApiResponse } from '../types/api'

/** 调用阶段一已定义的管理员认证接口。 */
export const authApi = {
  async login(username: string, password: string): Promise<AdminLoginResult> {
    const response = await http.post<ApiResponse<AdminLoginResult>>('/api/admin/auth/login', {
      username,
      password,
    })
    return response.data.data
  },

  async me(): Promise<AdminProfile> {
    const response = await http.get<ApiResponse<AdminProfile>>('/api/admin/auth/me')
    return response.data.data
  },
}
