/** P01/P02/P08/T03 小程序预订、积分试算与模拟支付调用。 */
import { request } from './request'
import type { MockPayResult, PaymentState, PointsPreview, PrepayResult, WechatPrepayResult } from '../types/booking'

export function previewPoints(data: {
  room_id: number
  check_in: string
  check_out: string
  points_used?: number
}): Promise<PointsPreview> {
  return request<PointsPreview>({ url: '/api/app/points/preview', method: 'POST', data })
}

export function createPrepay(data: {
  room_id: number
  check_in: string
  check_out: string
  guest_name: string
  phone: string
  guest_count: number
  remark?: string
  points_used: number
}): Promise<PrepayResult> {
  return request<PrepayResult>({ url: '/api/app/payments/prepay', method: 'POST', data })
}

export function getPaymentState(paymentNo: string): Promise<PaymentState> {
  return request<PaymentState>({ url: `/api/app/payments/${encodeURIComponent(paymentNo)}` })
}

export function mockPay(paymentNo: string): Promise<MockPayResult> {
  return request<MockPayResult>({
    url: `/api/app/payments/${encodeURIComponent(paymentNo)}/mock-pay`,
    method: 'POST',
  })
}

export function createWechatPrepay(
  paymentNo: string,
  data: { login_code: string },
): Promise<WechatPrepayResult> {
  return request<WechatPrepayResult>({
    url: `/api/app/payments/${encodeURIComponent(paymentNo)}/wechat-prepay`,
    method: 'POST',
    data,
  })
}
