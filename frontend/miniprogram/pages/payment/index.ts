/** 模拟支付页面：服务端状态决定结果，过期或失败不显示支付成功。 */
import { getPaymentState, mockPay } from '../../services/booking'
import type { PaymentStatus } from '../../types/booking'

interface PaymentPageData {
  paymentNo: string
  roomAmount: string
  pointsUsed: number
  discountAmount: string
  payAmount: string
  expiresAt: string
  countdown: string
  status: PaymentStatus | ''
  checking: boolean
  paying: boolean
  message: string
}

Page({
  data: {
    paymentNo: '',
    roomAmount: '0.00',
    pointsUsed: 0,
    discountAmount: '0.00',
    payAmount: '0.00',
    expiresAt: '',
    countdown: '--:--',
    status: '',
    checking: false,
    paying: false,
    message: '',
  } as PaymentPageData,

  timerId: 0,
  expirationQueried: false,

  onLoad(options: Record<string, string | undefined>) {
    const paymentNo = options.payment_no || ''
    if (!paymentNo || !options.expires_at) {
      this.setData({ message: '支付信息无效，请从预订页面重新操作' })
      return
    }
    this.setData({
      paymentNo,
      roomAmount: options.room_amount || '0.00',
      pointsUsed: Number(options.points_used || 0),
      discountAmount: options.discount_amount || '0.00',
      payAmount: options.pay_amount || '0.00',
      expiresAt: options.expires_at,
    })
    wx.setNavigationBarTitle({ title: '模拟支付' })
    this.updateCountdown()
    this.timerId = setInterval(() => this.updateCountdown(), 1000) as unknown as number
    void this.refreshPaymentState()
  },

  onUnload() {
    if (this.timerId) clearInterval(this.timerId)
  },

  async updateCountdown() {
    const remaining = Math.max(0, Date.parse(this.data.expiresAt) - Date.now())
    const totalSeconds = Math.ceil(remaining / 1000)
    const minutes = Math.floor(totalSeconds / 60)
    const seconds = totalSeconds % 60
    this.setData({ countdown: `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}` })
    if (remaining === 0 && !this.expirationQueried && this.data.status !== 'SUCCESS' && this.data.status !== 'CLOSED') {
      this.expirationQueried = true
      await this.refreshPaymentState()
    }
  },

  async refreshPaymentState() {
    if (!this.data.paymentNo || this.data.checking) return
    this.setData({ checking: true })
    try {
      const result = await getPaymentState(this.data.paymentNo)
      this.setData({
        status: result.status,
        message: result.status === 'CLOSED' ? '支付时间已结束，锁房与积分预占已释放。' : '',
      })
      if (result.status === 'SUCCESS' && result.order_id && result.order_no) {
        this.goToSuccess(result.order_id, result.order_no, this.data.payAmount)
      }
    } catch (error) {
      this.setData({ message: error instanceof Error ? error.message : '支付状态查询失败' })
    } finally {
      this.setData({ checking: false })
    }
  },

  async onPayTap() {
    if (this.data.paying || this.data.status === 'CLOSED' || this.data.status === 'FAILED') return
    if (Date.parse(this.data.expiresAt) <= Date.now()) {
      await this.refreshPaymentState()
      wx.showToast({ title: '支付时间已结束，请重新预订', icon: 'none' })
      return
    }
    this.setData({ paying: true, message: '' })
    try {
      const result = await mockPay(this.data.paymentNo)
      if (result.status !== 'SUCCESS' || !result.order_id || !result.order_no) {
        await this.refreshPaymentState()
        return
      }
      const confirmed = await getPaymentState(this.data.paymentNo)
      if (confirmed.status !== 'SUCCESS' || !confirmed.order_id || !confirmed.order_no) {
        this.setData({ status: confirmed.status, message: '支付结果尚未确认，请查询订单状态。' })
        return
      }
      this.goToSuccess(confirmed.order_id, confirmed.order_no, result.paid_amount)
    } catch (error) {
      this.setData({ message: error instanceof Error ? error.message : '模拟支付未成功，请查询支付状态' })
      await this.refreshPaymentState()
    } finally {
      this.setData({ paying: false })
    }
  },

  goToSuccess(orderId: number, orderNo: string, paidAmount: string) {
    wx.redirectTo({
      url: `/pages/payment-success/index?order_id=${orderId}&order_no=${encodeURIComponent(orderNo)}&paid_amount=${encodeURIComponent(paidAmount)}`,
    })
  },
})
