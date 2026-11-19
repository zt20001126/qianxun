/** P01/P02/P03/P08、T03 和 O01/O02/O03 的小程序接口结构。 */
export interface PointsPreview {
  room_amount: string
  available_points: number
  points_per_yuan: number
  max_points_usable: number
  points_used: number
  discount_amount: string
  pay_amount: string
}

export interface PrepayResult {
  booking_id: number
  payment_no: string
  room_amount: string
  points_used: number
  discount_amount: string
  pay_amount: string
  expires_at: string
}

export type PaymentStatus = 'PENDING' | 'SUCCESS' | 'FAILED' | 'CLOSED'

export interface PaymentState {
  payment_no: string
  status: PaymentStatus
  order_id: number | null
  order_no: string | null
}

export interface MockPayResult extends PaymentState {
  paid_amount: string
}

export type OrderStatus = 'PENDING_CONFIRM' | 'PENDING_CHECKIN' | 'CHECKED_IN' | 'COMPLETED' | 'CANCELLED'

export interface AppOrder {
  id: number
  order_no: string
  room_id: number
  room_number: string
  channel_id: number
  creator_id: number | null
  guest_name: string | null
  phone: string | null
  check_in: string
  check_out: string
  nights: number
  status: OrderStatus
  paid_amount: string
}

export interface AppOrderDetail extends Omit<AppOrder, 'channel_id' | 'creator_id'> {
  guest_count: number | null
  remark: string | null
  room_amount: string
  points_used: number
  discount_amount: string
  created_at: string
  updated_at: string
}

/** O03 自助取消资格查询结果。 */
export interface OrderCancellationEligibility {
  eligible: boolean
  reason: string | null
  deadline: string
}

/** P03 同步模拟退款结果。 */
export interface RefundResult {
  refund_id: number
  refund_no: string
  status: 'PENDING' | 'SUCCESS' | 'FAILED'
}

export interface AppOrderPage {
  list: AppOrder[]
  total: number
  page: number
  size: number
}

export function getOrderStatusLabel(status: OrderStatus): string {
  const labels: Record<OrderStatus, string> = {
    PENDING_CONFIRM: '待确认',
    PENDING_CHECKIN: '待入住',
    CHECKED_IN: '已入住',
    COMPLETED: '已完成',
    CANCELLED: '已取消',
  }
  return labels[status]
}
