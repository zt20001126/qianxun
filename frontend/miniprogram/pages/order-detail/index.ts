/** O02 小程序本人订单详情。 */
import { getCancellationEligibility, getMyOrder, refundMyOrder } from '../../services/orders'
import type { AppOrderDetail, OrderCancellationEligibility } from '../../types/booking'
import { getOrderStatusLabel } from '../../types/booking'
import { STORE_CONTACT_PHONE } from '../../config'

interface OrderDetailPageData {
  order: AppOrderDetail | null
  statusLabel: string
  loading: boolean
  errorMessage: string
  cancellation: OrderCancellationEligibility | null
  cancelConfirming: boolean
  refundSubmitting: boolean
}

Page({
  data: {
    order: null,
    statusLabel: '',
    loading: true,
    errorMessage: '',
    cancellation: null,
    cancelConfirming: false,
    refundSubmitting: false,
  } as OrderDetailPageData,

  async onLoad(options: Record<string, string | undefined>) {
    const id = Number(options.id)
    if (!Number.isInteger(id) || id <= 0) {
      this.setData({ loading: false, errorMessage: '订单编号无效' })
      return
    }
    await this.loadOrder(id)
  },

  async onPullDownRefresh() {
    if (this.data.order) await this.loadOrder(this.data.order.id)
    wx.stopPullDownRefresh()
  },

  async loadOrder(id: number) {
    this.setData({ loading: true, errorMessage: '' })
    try {
      const order = await getMyOrder(id)
      const cancellation = order.status === 'CANCELLED'
        ? null
        : await getCancellationEligibility(id).catch(() => ({
            eligible: false,
            reason: '暂时无法读取取消资格，请稍后重试或联系商家',
            deadline: '',
          }))
      this.setData({ order, cancellation, statusLabel: getOrderStatusLabel(order.status) })
      wx.setNavigationBarTitle({ title: `${order.room_number} 房订单` })
    } catch (error) {
      this.setData({ errorMessage: error instanceof Error ? error.message : '订单详情读取失败' })
    } finally {
      this.setData({ loading: false })
    }
  },

  onOrdersTap() {
    wx.switchTab({ url: '/pages/orders/index' })
  },

  onHomeTap() {
    wx.switchTab({ url: '/pages/home/index' })
  },

  async onCancelTap() {
    const { order, cancellation } = this.data
    if (!order || order.status === 'CANCELLED' || this.data.cancelConfirming || this.data.refundSubmitting) return
    if (!cancellation?.eligible) {
      this.setData({ cancelConfirming: true })
      try {
        const result = await wx.showModal({
          title: '暂不支持自助退款',
          content: `${cancellation?.reason || '当前订单不支持自助退款，请联系商家'}\n商家电话：${STORE_CONTACT_PHONE}`,
          confirmText: '拨打商家',
          cancelText: '我知道了',
        })
        if (result.confirm) wx.makePhoneCall({ phoneNumber: STORE_CONTACT_PHONE })
      } finally {
        this.setData({ cancelConfirming: false })
      }
      return
    }

    this.setData({ cancelConfirming: true })
    let confirmation: WechatMiniprogram.ShowModalSuccessCallbackResult
    try {
      confirmation = await wx.showModal({
        title: '取消订单并退款',
        content: `将同步模拟退还实付金额 ¥${order.paid_amount}，并返还本单使用的积分。确认取消吗？`,
        confirmText: '确认取消',
        cancelText: '再想想',
      })
    } finally {
      this.setData({ cancelConfirming: false })
    }
    if (!confirmation.confirm) return

    this.setData({ refundSubmitting: true })
    try {
      await refundMyOrder(order.id)
      wx.showToast({ title: '订单已取消', icon: 'success' })
      await this.loadOrder(order.id)
    } catch (error) {
      wx.showToast({ title: error instanceof Error ? error.message : '退款未完成，请稍后重试', icon: 'none' })
      await this.loadOrder(order.id)
    } finally {
      this.setData({ refundSubmitting: false })
    }
  },
})
