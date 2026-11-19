import { http } from './http'
import type { ApiResponse } from '../types/api'
import type { Expense, ExpenseCreateRequest, ExpensePage, ExpenseSummary } from '../types/operations'

export interface ExpenseFilters {
  start_date?: string
  end_date?: string
  keyword?: string
  page?: number
  size?: number
}

export const expenseApi = {
  async list(filters: ExpenseFilters): Promise<ExpensePage> {
    const response = await http.get<ApiResponse<ExpensePage>>('/api/admin/expenses', { params: filters })
    return response.data.data
  },

  async get(id: number): Promise<Expense> {
    const response = await http.get<ApiResponse<Expense>>('/api/admin/expenses/' + id)
    return response.data.data
  },

  async create(request: ExpenseCreateRequest): Promise<Expense> {
    const response = await http.post<ApiResponse<Expense>>('/api/admin/expenses', request)
    return response.data.data
  },

  async summary(startDate: string, endDate: string): Promise<ExpenseSummary> {
    const response = await http.get<ApiResponse<ExpenseSummary>>('/api/admin/expenses/summary', {
      params: { start_date: startDate, end_date: endDate },
    })
    return response.data.data
  },
}
