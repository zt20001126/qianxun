/** 后端统一 API 响应结构。 */
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

/** A03/A04 管理员公开资料。 */
export interface AdminProfile {
  id: number
  username: string
}

/** A03 管理员登录结果。 */
export interface AdminLoginResult {
  token: string
  admin: AdminProfile
}
