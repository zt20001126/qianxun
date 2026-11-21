/** 模拟支付页面：服务端状态决定结果，过期或失败不显示支付成功。 */
import { createWechatPrepay, getPaymentState, mockPay } from '../../services/booking'
import type { PaymentMethod, PaymentStatus } from '../../types/booking'

interface PaymentPageData {
  paymentNo: string
  paymentMethod: PaymentMethod
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

function normalizeExpiresAt(value: string): string {
  let decoded = value
  try {
    decoded = decodeURIComponent(value)
  } catch {
    // 保留原值，后续倒计时会用占位符处理无法解析的时间。
  }
  // 某些路由解析会把偏移量中的 + 解成空格，恢复为 ISO 8601 正偏移。
  return decoded.replace(/(T\d{2}:\d{2}:\d{2}(?:\.\d+)?)[ ](\d{2}:\d{2})$/, '$1+$2')
}

Page({
  data: {
    paymentNo: '',
    paymentMethod: 'MOCK',
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
      paymentMethod: options.payment_method === 'WECHAT' ? 'WECHAT' : 'MOCK',
      roomAmount: options.room_amount || '0.00',
      pointsUsed: Number(options.points_used || 0),
      discountAmount: options.discount_amount || '0.00',
      payAmount: options.pay_amount || '0.00',
      expiresAt: normalizeExpiresAt(options.expires_at),
    })
    wx.setNavigationBarTitle({ title: options.payment_method === 'WECHAT' ? '微信支付' : '模拟支付' })
    this.updateCountdown()
    this.timerId = setInterval(() => this.updateCountdown(), 1000) as unknown as number
    void this.refreshPaymentState()
  },

  onUnload() {
    if (this.timerId) clearInterval(this.timerId)
  },

  async updateCountdown() {
    const expiresAt = Date.parse(this.data.expiresAt)
    if (!Number.isFinite(expiresAt)) {
      this.setData({ countdown: '--:--' })
      if (!this.expirationQueried) {
        this.expirationQueried = true
        await this.refreshPaymentState()
      }
      return
    }
    const remaining = Math.max(0, expiresAt - Date.now())
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
    if (this.data.paymentMethod === 'WECHAT') return
    if (this.data.paying || this.data.status === 'CLOSED' || this.data.status === 'FAILED') return
    const expiresAt = Date.parse(this.data.expiresAt)
    if (Number.isFinite(expiresAt) && expiresAt <= Date.now()) {
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

  async onWechatPayTap() {
    if (this.data.paymentMethod !== 'WECHAT' || this.data.paying || this.data.checking) return
    this.setData({ paying: true, message: '' })
    try {
      const loginCode = await new Promise<string>((resolve, reject) => {
        wx.login({
          success: (result) => result.code ? resolve(result.code) : reject(new Error('获取微信登录凭证失败')),
          fail: () => reject(new Error('获取微信登录凭证失败，请重试')),
        })
      })
      const payment = await createWechatPrepay(this.data.paymentNo, {
        login_code: loginCode,
      })
      await new Promise<void>((resolve, reject) => {
        wx.requestPayment({
          timeStamp: payment.time_stamp,
          nonceStr: payment.nonce_str,
          package: payment.package,
          signType: payment.sign_type as 'RSA',
          paySign: payment.pay_sign,
          success: () => resolve(),
          fail: (error) => reject(new Error(error.errMsg?.includes('cancel') ? '已取消支付' : '微信支付未完成')),
        })
      })
      await this.confirmWechatPayment()
    } catch (error) {
      this.setData({ message: error instanceof Error ? error.message : '微信支付未完成，请查询支付状态' })
      await this.refreshPaymentState()
    } finally {
      this.setData({ paying: false })
    }
  },

  async confirmWechatPayment() {
    // 小程序支付成功回调只代表客户端完成；正式订单以服务端支付通知结算结果为准。
    for (let attempt = 0; attempt < 10; attempt += 1) {
      const result = await getPaymentState(this.data.paymentNo)
      this.setData({ status: result.status })
      if (result.status === 'SUCCESS' && result.order_id && result.order_no) {
        this.goToSuccess(result.order_id, result.order_no, this.data.payAmount)
        return
      }
      if (result.status === 'CLOSED' || result.status === 'FAILED') break
      await new Promise((resolve) => setTimeout(resolve, 1000))
    }
    this.setData({ message: '微信已返回支付结果，订单仍在确认中，请稍后查询支付状态。' })
  },

  goToSuccess(orderId: number, orderNo: string, paidAmount: string) {
    wx.redirectTo({
      url: `/pages/payment-success/index?payment_no=${encodeURIComponent(this.data.paymentNo)}&order_id=${orderId}&order_no=${encodeURIComponent(orderNo)}&paid_amount=${encodeURIComponent(paidAmount)}`,
    })
  },
})
