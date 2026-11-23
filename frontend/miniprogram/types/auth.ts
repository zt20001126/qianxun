/** A01/A02 返回的当前会员资料。 */
export interface MemberProfile {
  id: number
  phone: string
  nickname: string
  avatar: string
  points: number
  growth_points: number
}

/** A01 微信手机号授权登录响应数据。 */
export interface AppLoginResult {
  token: string
  expires_in: number
  member: MemberProfile
}

/** 后端统一 API 响应结构。 */
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

/** 小程序全局数据类型。 */
export interface AppGlobalData {
  member: MemberProfile | null
  token: string
  tokenExpiresAt: number
}

/** 小程序应用类型定义。 */
export interface IAppOption {
  globalData: AppGlobalData
}
