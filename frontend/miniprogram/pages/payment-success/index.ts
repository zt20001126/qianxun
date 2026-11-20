/** 预订成功页：再次向服务端确认支付和正式订单状态。 */
import { getPaymentState } from '../../services/booking'

interface SuccessPageData {
  orderId: number
  orderNo: string
  paymentNo: string
  loading: boolean
  confirmed: boolean
}

Page({
  data: {
    orderId: 0,
    orderNo: '',
    paymentNo: '',
    loading: true,
    confirmed: false,
  } as SuccessPageData,

  async onLoad(options: Record<string, string | undefined>) {
    const orderId = Number(options.order_id)
    const orderNo = options.order_no || ''
    const paymentNo = options.payment_no || ''
    if (!Number.isInteger(orderId) || orderId <= 0 || !orderNo || !paymentNo) {
      this.returnHome()
      return
    }

    try {
      const payment = await getPaymentState(paymentNo)
      if (payment.status !== 'SUCCESS'
        || payment.order_id !== orderId
        || payment.order_no !== orderNo) {
        this.returnHome()
        return
      }
      this.setData({ orderId, orderNo, paymentNo, confirmed: true })
      wx.setNavigationBarTitle({ title: '预订成功' })
    } catch {
      wx.showToast({ title: '暂时无法确认支付结果', icon: 'none' })
      this.returnHome()
    } finally {
      this.setData({ loading: false })
    }
  },

  onCopyTap() {
    if (!this.data.confirmed || !this.data.orderNo) return
    wx.setClipboardData({
      data: this.data.orderNo,
      success: () => wx.showToast({ title: '已复制订单号', icon: 'success' }),
      fail: () => wx.showToast({ title: '复制失败，请长按订单号', icon: 'none' }),
    })
  },

  onDetailTap() {
    if (this.data.confirmed && this.data.orderId) {
      wx.redirectTo({ url: '/pages/order-detail/index?id=' + this.data.orderId })
    }
  },

  onHomeTap() {
    wx.switchTab({ url: '/pages/home/index' })
  },

  returnHome() {
    wx.switchTab({ url: '/pages/home/index' })
  },
})
