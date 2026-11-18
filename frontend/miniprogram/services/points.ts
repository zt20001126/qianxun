/** T01/T02 小程序积分规则及本人积分流水接口。 */
import { request } from './request'
import type { MemberPage, PointsLog, PointsRules } from '../types/member'

export function getPointsRules(): Promise<PointsRules> {
  return request<PointsRules>({ url: '/api/app/points/rules' })
}

export function listMyPointsLogs(page = 1, size = 20): Promise<MemberPage<PointsLog>> {
  return request<MemberPage<PointsLog>>({ url: `/api/app/points/logs?page=${page}&size=${size}` })
}
