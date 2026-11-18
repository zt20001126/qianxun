/** O01/O02 小程序本人订单查询。 */
import { request } from './request'
import type { AppOrderDetail, AppOrderPage } from '../types/booking'

export function listMyOrders(page = 1, size = 20): Promise<AppOrderPage> {
  return request<AppOrderPage>({ url: `/api/app/orders?page=${page}&size=${size}` })
}

export function getMyOrder(id: number): Promise<AppOrderDetail> {
  return request<AppOrderDetail>({ url: `/api/app/orders/${id}` })
}
