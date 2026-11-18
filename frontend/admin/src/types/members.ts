import type { OrderStatus } from './orders'

/** M03 后台会员列表项。 */
export interface MemberListItem {
  id: number
  phone: string
  nickname: string
  growth_points: number
  points: number
  level: string
  created_at: string
}

/** M04 会员关联订单摘要。 */
export interface MemberOrder {
  id: number
  order_no: string
  room_number: string
  check_in: string
  check_out: string
  status: OrderStatus
  paid_amount: string
}

/** M04 会员详情包含最近 20 条积分流水；更早记录通过 T04 分页查询。 */
export interface MemberDetail extends MemberListItem {
  avatar: string
  orders: MemberOrder[]
  points_logs: PointsLog[]
  points_log_total: number
}

/** M03 会员列表分页结构。 */
export interface MemberPage {
  list: MemberListItem[]
  total: number
  page: number
  size: number
}

export type PointsLogType = 'ORDER_REWARD' | 'HISTORY_REWARD' | 'GIFT' | 'REDEEM' | 'RETURN' | 'ADJUST'

/** T02/T04 积分流水列表项。 */
export interface PointsLog {
  id: number
  member_id: number
  order_id: number | null
  type: PointsLogType
  points: number
  growth_points: number
  amount: string | null
  discount_amount: string | null
  balance_after: number
  remark: string | null
  created_at: string
}

export interface PointsLogPage {
  list: PointsLog[]
  total: number
  page: number
  size: number
}

export interface PointsGrantRequest {
  type: 'HISTORY_REWARD' | 'GIFT'
  points: number
  order_id?: number
  remark?: string
  request_no: string
}

export interface PointsGrantResult {
  member_id: number
  points: number
  growth_points: number
  balance_after: number
  log_id: number
}

export interface PointsRewardRetryResult {
  order_id: number
  rewarded: boolean
  log_id: number | null
}
