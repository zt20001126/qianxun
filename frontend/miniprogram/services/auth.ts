/** 小程序认证接口调用，只访问阶段一 A01 与 A02。 */
import type { AppLoginResult, MemberProfile } from '../types/auth'
import { request } from './request'

export function login(loginCode: string, phoneCode: string): Promise<AppLoginResult> {
  return request<AppLoginResult>({
    url: '/api/app/auth/login',
    method: 'POST',
    data: { login_code: loginCode, phone_code: phoneCode },
    withAuth: false,
  })
}

export function getCurrentMember(): Promise<MemberProfile> {
  return request<MemberProfile>({ url: '/api/app/auth/me' })
}
