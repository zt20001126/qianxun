/** 支付完成页；仅在服务端确认 SUCCESS 且存在正式订单编号后进入。 */
Page({
  data: {
    orderId: 0,
    orderNo: '',
    paidAmount: '0.00',
  },

  onLoad(options: Record<string, string | undefined>) {
    const orderId = Number(options.order_id)
    const orderNo = options.order_no || ''
    if (!Number.isInteger(orderId) || orderId <= 0 || !orderNo) {
      wx.reLaunch({ url: '/pages/home/index' })
      return
    }
    this.setData({ orderId, orderNo, paidAmount: options.paid_amount || '0.00' })
    wx.setNavigationBarTitle({ title: '预订完成' })
  },

  onDetailTap() {
    if (this.data.orderId) wx.redirectTo({ url: `/pages/order-detail/index?id=${this.data.orderId}` })
  },

  onOrdersTap() {
    wx.redirectTo({ url: '/pages/orders/index' })
  },

  onHomeTap() {
    wx.reLaunch({ url: '/pages/home/index' })
  },
})
