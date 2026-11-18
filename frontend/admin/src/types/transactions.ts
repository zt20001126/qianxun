/** P04/P05 后台支付与退款流水结构。 */
export type PaymentStatus = 'PENDING' | 'SUCCESS' | 'FAILED' | 'CLOSED'
export type RefundStatus = 'PENDING' | 'SUCCESS' | 'FAILED'

export interface PaymentListItem {
  id: number
  payment_no: string
  order_id: number | null
  amount: string
  status: PaymentStatus
  paid_at: string | null
}

export interface RefundListItem {
  id: number
  refund_no: string
  order_id: number
  amount: string
  status: RefundStatus
  reason: string | null
  refunded_at: string | null
}

export interface TransactionPage<T> {
  list: T[]
  total: number
  page: number
  size: number
}

export interface TransactionFilters {
  status?: PaymentStatus | RefundStatus
  start_date?: string
  end_date?: string
  page: number
  size: number
}
