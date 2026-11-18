/** M01/M02 会员中心资料与消费订单接口。 */
import { request } from './request'
import type { AppOrderPage } from '../types/booking'
import type { AppMember } from '../types/member'

export function getMyMember(): Promise<AppMember> {
  return request<AppMember>({ url: '/api/app/members/me' })
}

export function listMyMemberOrders(page = 1, size = 20): Promise<AppOrderPage> {
  return request<AppOrderPage>({ url: `/api/app/members/me/orders?page=${page}&size=${size}` })
}
