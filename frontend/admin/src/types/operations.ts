/** 阶段九经营支出、经营总览与月报接口类型。 */
export interface Expense {
  id: number
  content: string
  amount: string
  date: string
  created_at: string
  updated_at: string
}

export interface ExpensePage {
  list: Expense[]
  total: number
  page: number
  size: number
}

export interface ExpenseDailyAmount {
  date: string
  amount: string
}

export interface ExpenseSummary {
  total_amount: string
  count: number
  daily: ExpenseDailyAmount[]
}

export interface ExpenseCreateRequest {
  content: string
  amount: string
  date: string
}

export type ReportScope = 'ALL' | 'NOT_CHECKED_IN' | 'CHECKED_IN'
export type ReportDimension = 'overview' | 'channels' | 'creators' | 'rooms'

export interface DashboardRoom {
  room_id: number
  room_number: string
  room_name: string
  status: string
  order_id: number | null
  guest_name?: string | null
  guest_count?: number | null
  check_in?: string | null
  check_out?: string | null
  channel_name?: string | null
}

export interface DashboardMonthSummary {
  unoccupied_order_revenue: string
  checked_in_order_revenue: string
  platform_fee: string
  platform_commission: string
  extra_commission: string
  net_amount: string
  expense_amount: string
  operating_profit: string
  occupancy_rate: number | null
}

export interface Dashboard {
  date: string
  occupied_rooms: number
  total_rooms: number
  rooms: DashboardRoom[]
  month_summary: DashboardMonthSummary
}

export interface ReportOverview {
  month: string
  scope: ReportScope
  order_count: number
  night_count: number
  paid_amount: string
  platform_fee: string
  platform_commission: string
  extra_commission: string
  net_amount: string
  unoccupied_order_revenue: string
  checked_in_order_revenue: string
  expense_amount: string
  operating_profit: string
  occupancy_rate: number | null
}

export interface ReportDimensionItem {
  id: number | null
  name: string
  order_count: number
  night_count: number
  paid_amount: string
  platform_fee: string
  platform_commission: string
  extra_commission: string
  total_commission: string
  net_amount: string
  room_profit?: string | null
  occupancy_rate?: number | null
}

export interface ReportDimensionResult {
  month: string
  scope: ReportScope
  list: ReportDimensionItem[]
}
