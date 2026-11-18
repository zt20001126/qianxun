/** M01 会员中心资料。 */
export interface AppMember {
  id: number
  phone: string
  nickname: string
  avatar: string
  growth_points: number
  points: number
  level: string
  next_level_points: number
  progress_percent: number
}

/** T01 单个等级门槛及兑换比例。 */
export interface PointsLevelRule {
  name: string
  growth_points: number
  points_per_yuan: number
}

/** T01 积分规则。 */
export interface PointsRules {
  levels: PointsLevelRule[]
  max_discount_rate: number
  expire: boolean
}

/** M02/T02 分页数据。 */
export interface MemberPage<T> {
  list: T[]
  total: number
  page: number
  size: number
}

/** T02 积分流水记录。 */
export interface PointsLog {
  id: number
  member_id: number
  order_id: number | null
  type: 'ORDER_REWARD' | 'HISTORY_REWARD' | 'GIFT' | 'REDEEM' | 'RETURN' | 'ADJUST'
  points: number
  growth_points: number
  amount: string | null
  discount_amount: string | null
  balance_after: number
  remark: string | null
  created_at: string
}
