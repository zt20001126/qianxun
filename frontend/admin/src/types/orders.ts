import type { BookerPortrait, BookerPortraitInput } from './portraits'

/** 订单接口类型与管理端订单扩展契约保持一致。 */
export type OrderStatus =
  | 'PENDING_CONFIRM'
  | 'PENDING_CHECKIN'
  | 'CHECKED_IN'
  | 'COMPLETED'
  | 'CANCELLED'

export const orderStatusLabels: Record<OrderStatus, string> = {
  PENDING_CONFIRM: '待确认',
  PENDING_CHECKIN: '待入住',
  CHECKED_IN: '已入住',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
}

export interface OrderListItem {
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
  platform_fee: string
  platform_commission: string
  extra_commission: string
  expected_amount: string | null
  net_amount: string
}

export interface OrderDetail extends OrderListItem {
  platform_order_no: string | null
  channel_name: string
  member_id: number | null
  guest_count: number | null
  remark: string | null
  admin_remark: string | null
  booker_portrait: BookerPortrait | null
  created_at: string
  updated_at: string
}

export interface OrderPage {
  list: OrderListItem[]
  total: number
  page: number
  size: number
}

export interface OrderFilters {
  channel_id?: number
  room_id?: number
  creator_id?: number
  status?: OrderStatus
  check_in_from?: string
  check_in_to?: string
  guest_name?: string
  phone?: string
  page: number
  size: number
}

export interface CreateExternalOrderRequest {
  channel_id: number
  room_id: number
  creator_id: number | null
  platform_order_no: string | null
  guest_name: string | null
  phone: string | null
  guest_count: number | null
  check_in: string
  check_out: string
  status: Exclude<OrderStatus, 'CANCELLED'>
  remark: string | null
  admin_remark: string | null
  booker_portrait: BookerPortraitInput
  amount: {
    paid_amount: string
    platform_fee: string
    platform_commission: string
    expected_amount: string | null
  }
}

export interface UpdateOrderRequest {
  guest_name?: string | null
  phone?: string | null
  guest_count?: number | null
  remark?: string | null
  admin_remark?: string | null
  booker_portrait?: BookerPortraitInput
  room_id?: number
  check_in?: string
  check_out?: string
}
