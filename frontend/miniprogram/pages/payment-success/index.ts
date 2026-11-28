/** 支付成功页：兼容客房预订与商城购买，并再次向服务端确认支付结果。 */
import { getPaymentState } from '../../services/booking'
import { getMallPaymentStatus } from '../../services/mall'

interface SuccessPageData {
  orderId: number
  orderNo: string
  paymentNo: string
  isMall: boolean
  loadingText: string
  successTitle: string
  successCopyFirst: string
  successCopySecond: string
  loading: boolean
  confirmed: boolean
  errorMessage: string
}

Page({
  data: {
    orderId: 0,
    orderNo: '',
    paymentNo: '',
    isMall: false,
    loadingText: '正在确认预订结果…',
    successTitle: '预订成功',
    successCopyFirst: '您的房间已预订，',
    successCopySecond: '可在「我的订单」中查看。',
    loading: true,
    confirmed: false,
    errorMessage: '',
  } as SuccessPageData,

  onLoad(options: Record<string, string | undefined>) {
    const isMall = options.source === 'mall'
    const orderId = Number(options.order_id)
    const orderNo = options.order_no || ''
    const paymentNo = options.payment_no || ''
    if (!Number.isInteger(orderId) || orderId <= 0 || !orderNo || !paymentNo) {
      this.returnHome()
      return
    }

    this.setData({
      orderId, orderNo, paymentNo, isMall, loading: false,
      loadingText: isMall ? '正在确认购买结果…' : '正在确认预订结果…',
    })
    return this.onRetryTap()
  },

  /** 仅重查已有交易，网络恢复后可继续确认原订单。 */
  async onRetryTap() {
    const { orderId, orderNo, paymentNo, isMall, loading, confirmed } = this.data
    if (loading || confirmed || !paymentNo) return
    this.setData({ loading: true, errorMessage: '' })
    try {
      const payment = isMall ? await getMallPaymentStatus(paymentNo) : await getPaymentState(paymentNo)
      if (payment.status !== 'SUCCESS' || payment.order_id !== orderId || payment.order_no !== orderNo) {
        this.setData({ errorMessage: '暂时无法确认支付结果，请稍后查询' })
        return
      }
      this.setData({
        successTitle: isMall ? '购买成功' : '预订成功',
        successCopyFirst: isMall ? '您的芊浔好物已购买，' : '您的房间已预订，',
        successCopySecond: isMall ? '可在「商城订单」中查看。' : '可在「我的订单」中查看。',
        confirmed: true,
      })
      wx.setNavigationBarTitle({ title: isMall ? '购买成功' : '预订成功' })
    } catch {
      this.setData({ errorMessage: '暂时无法确认支付结果，请检查网络后重试' })
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
      if (this.data.isMall) {
        wx.redirectTo({ url: '/pages/mall-order-detail/index?id=' + this.data.orderId })
        return
      }
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
