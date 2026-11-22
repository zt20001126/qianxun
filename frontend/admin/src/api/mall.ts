import { http } from './http'
import type { ApiResponse } from '../types/api'

/** 商城后台列表统一每页展示的记录数。 */
export const MALL_PAGE_SIZE = 10

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

/** 商城订单列表的服务端查询条件。 */
export interface MallOrderQuery {
  page: number
  size?: number
  status?: MallOrder['status']
  keyword?: string
}

export interface MallOrderPage {
  list: MallOrder[]
  total: number
  page: number
  size: number
}

/** 商城商品管理 API，沿用后台统一认证与错误处理。 */
export const mallApi = {
  async list(page: number, size = MALL_PAGE_SIZE): Promise<MallProductPage> {
    const result = await http.get<ApiResponse<MallProductPage>>('/api/admin/mall/products', { params: { page, size } })
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
  async orders(query: MallOrderQuery): Promise<MallOrderPage> {
    const result = await http.get<ApiResponse<MallOrderPage>>('/api/admin/mall/orders', {
      params: { ...query, size: query.size ?? MALL_PAGE_SIZE },
    })
    return result.data.data
  },
  async ship(id: number, express_company: string, tracking_no: string): Promise<void> {
    await http.post(`/api/admin/mall/orders/${id}/ship`, { express_company, tracking_no })
  },
  async complete(id: number): Promise<void> {
    await http.post(`/api/admin/mall/orders/${id}/complete`)
  },
}
