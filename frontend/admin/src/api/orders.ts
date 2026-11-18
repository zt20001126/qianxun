import { http } from './http'
import type { ApiResponse } from '../types/api'
import type {
  CreateExternalOrderRequest,
  OrderDetail,
  OrderFilters,
  OrderPage,
  OrderStatus,
  UpdateOrderRequest,
} from '../types/orders'

/** O04-O12 后台订单接口。 */
export const orderApi = {
  async list(filters: OrderFilters): Promise<OrderPage> {
    const response = await http.get<ApiResponse<OrderPage>>('/api/admin/orders', { params: filters })
    return response.data.data
  },

  async detail(id: number): Promise<OrderDetail> {
    const response = await http.get<ApiResponse<OrderDetail>>(`/api/admin/orders/${id}`)
    return response.data.data
  },

  async create(request: CreateExternalOrderRequest): Promise<{ id: number; order_no: string; net_amount: string }> {
    const response = await http.post<ApiResponse<{ id: number; order_no: string; net_amount: string }>>(
      '/api/admin/orders', request,
    )
    return response.data.data
  },

  async update(id: number, request: UpdateOrderRequest): Promise<OrderDetail> {
    const response = await http.put<ApiResponse<OrderDetail>>(`/api/admin/orders/${id}`, request)
    return response.data.data
  },

  async confirm(id: number): Promise<{ id: number; status: OrderStatus; updated_at: string }> {
    const response = await http.post<ApiResponse<{ id: number; status: OrderStatus; updated_at: string }>>(
      `/api/admin/orders/${id}/confirm`,
    )
    return response.data.data
  },

  async checkIn(id: number): Promise<{ id: number; status: OrderStatus; updated_at: string }> {
    const response = await http.post<ApiResponse<{ id: number; status: OrderStatus; updated_at: string }>>(
      `/api/admin/orders/${id}/check-in`,
    )
    return response.data.data
  },

  async checkOut(id: number): Promise<{ id: number; status: OrderStatus; updated_at: string }> {
    const response = await http.post<ApiResponse<{ id: number; status: OrderStatus; updated_at: string }>>(
      `/api/admin/orders/${id}/check-out`,
    )
    return response.data.data
  },

  async cancel(id: number, reason: string): Promise<{ id: number; status: OrderStatus; updated_at: string }> {
    const response = await http.post<ApiResponse<{ id: number; status: OrderStatus; updated_at: string }>>(
      `/api/admin/orders/${id}/cancel`, reason ? { reason } : {},
    )
    return response.data.data
  },
}
