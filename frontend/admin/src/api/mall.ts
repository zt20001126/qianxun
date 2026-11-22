import { http } from './http'
import type { ApiResponse } from '../types/api'

/** 商城商品维护所需的数据。 */
export interface MallProduct {
  id: number
  name: string
  description: string
  images: string[]
  price: string
  status: 'ON_SHELF' | 'OFF_SHELF'
}

export interface MallProductInput {
  name: string
  description: string
  images: string[]
  price: string
}
export interface MallProductPage { list: MallProduct[]; total: number; page: number; size: number }

/** 管理员查看的商品订单。 */
export interface MallOrder {
  id: number
  order_no: string
  product_name: string
  quantity: number
  paid_amount: string
  recipient_name: string
  recipient_phone: string
  shipping_address: string
  status: 'PENDING_SHIPMENT' | 'SHIPPED' | 'COMPLETED' | 'REFUNDED'
  express_company: string | null
  tracking_no: string | null
}

export interface MallOrderPage {
  list: MallOrder[]
  total: number
  page: number
  size: number
}

/** 商城商品管理 API，沿用后台统一认证与错误处理。 */
export const mallApi = {
  async list(page: number): Promise<MallProductPage> {
    const result = await http.get<ApiResponse<MallProductPage>>('/api/admin/mall/products', { params: { page, size: 20 } })
    return result.data.data
  },
  async create(input: MallProductInput): Promise<void> {
    await http.post('/api/admin/mall/products', input)
  },
  async update(id: number, input: MallProductInput): Promise<void> {
    await http.put(`/api/admin/mall/products/${id}`, input)
  },
  async setStatus(id: number, status: MallProduct['status']): Promise<void> {
    await http.post(`/api/admin/mall/products/${id}/status`, { status })
  },
  async orders(page: number): Promise<MallOrderPage> {
    const result = await http.get<ApiResponse<MallOrderPage>>('/api/admin/mall/orders', { params: { page, size: 20 } })
    return result.data.data
  },
  async ship(id: number, express_company: string, tracking_no: string): Promise<void> {
    await http.post(`/api/admin/mall/orders/${id}/ship`, { express_company, tracking_no })
  },
  async complete(id: number): Promise<void> {
    await http.post(`/api/admin/mall/orders/${id}/complete`)
  },
}
