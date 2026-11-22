/** 商城商品及当前会员商品订单查询。 */
import { request } from './request'

export interface MallProduct {
  id: number
  name: string
  description: string
  images: string[]
  price: string
}

export interface MallOrder {
  id: number
  order_no: string
  product_name: string
  quantity: number
  paid_amount: string
  status: 'PENDING_SHIPMENT' | 'SHIPPED' | 'COMPLETED' | 'REFUNDED'
  express_company: string | null
  tracking_no: string | null
}

export interface MallOrderPage { list: MallOrder[]; total: number; page: number; size: number }
export interface MallShipping { recipient_name: string | null; recipient_phone: string | null; shipping_address: string | null }
export interface MallPreview {
  total_amount: string; available_points: number; points_per_yuan: number; max_points_usable: number
  points_used: number; discount_amount: string; amount: string
}
export interface MallPrepay { payment_no: string; payment_method: 'MOCK' | 'WECHAT'; total_amount: string;
  points_used: number; discount_amount: string; amount: string; expires_at: string }

export function listMallProducts(page: number): Promise<{ list: MallProduct[]; total: number; page: number; size: number }> {
  return request({ url: `/api/app/mall/products?page=${page}&size=20` })
}

export function getMallProduct(id: number): Promise<MallProduct> {
  return request({ url: `/api/app/mall/products/${id}` })
}
export function getMallShipping(): Promise<MallShipping> {
  return request({ url: '/api/app/mall/payments/shipping' })
}
export function previewMall(product_id: number, quantity: number, points_used: number): Promise<MallPreview> {
  return request({ url: '/api/app/mall/payments/preview', method: 'POST', data: { product_id, quantity, points_used } })
}
export function prepayMall(data: { product_id: number; quantity: number; points_used: number;
  recipient_name: string; recipient_phone: string; shipping_address: string }): Promise<MallPrepay> {
  return request({ url: '/api/app/mall/payments', method: 'POST', data })
}
export function mockPayMall(paymentNo: string): Promise<{ status: string; order_id: number }> {
  return request({ url: `/api/app/mall/payments/${encodeURIComponent(paymentNo)}/mock-pay`, method: 'POST' })
}
export function createMallWechatPrepay(paymentNo: string, loginCode: string, phoneCode: string): Promise<{
  time_stamp: string; nonce_str: string; package: string; sign_type: string; pay_sign: string
}> {
  return request({ url: `/api/app/mall/payments/${encodeURIComponent(paymentNo)}/wechat-prepay`, method: 'POST',
    data: { login_code: loginCode, phone_code: phoneCode } })
}
export function getMallPaymentStatus(paymentNo: string): Promise<{ status: string; order_id: number | null }> {
  return request({ url: `/api/app/mall/payments/${encodeURIComponent(paymentNo)}` })
}

export function listMallOrders(page: number): Promise<MallOrderPage> {
  return request<MallOrderPage>({ url: `/api/app/mall/orders?page=${page}&size=20` })
}

/** 申请本人待发货订单整单退款。 */
export function refundMallOrder(id: number): Promise<{ refund_no: string; status: string }> {
  return request({ url: `/api/app/mall/orders/${id}/refund`, method: 'POST' })
}
