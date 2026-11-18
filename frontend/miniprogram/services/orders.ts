/** O01/O02/O03/P03 小程序本人订单查询、取消资格和退款接口。 */
import { request } from './request'
import type { AppOrderDetail, AppOrderPage, OrderCancellationEligibility, RefundResult } from '../types/booking'

export function listMyOrders(page = 1, size = 20): Promise<AppOrderPage> {
  return request<AppOrderPage>({ url: `/api/app/orders?page=${page}&size=${size}` })
}

export function getMyOrder(id: number): Promise<AppOrderDetail> {
  return request<AppOrderDetail>({ url: `/api/app/orders/${id}` })
}

export function getCancellationEligibility(id: number): Promise<OrderCancellationEligibility> {
  return request<OrderCancellationEligibility>({ url: `/api/app/orders/${id}/cancel-eligibility` })
}

export function refundMyOrder(id: number): Promise<RefundResult> {
  return request<RefundResult>({ method: 'POST', url: `/api/app/orders/${id}/refunds` })
}
