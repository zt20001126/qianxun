/** O02 小程序本人订单详情。 */
import { getMyOrder } from '../../services/orders'
import type { AppOrderDetail } from '../../types/booking'
import { getOrderStatusLabel } from '../../types/booking'

interface OrderDetailPageData {
  order: AppOrderDetail | null
  statusLabel: string
  loading: boolean
  errorMessage: string
}

Page({
  data: { order: null, statusLabel: '', loading: true, errorMessage: '' } as OrderDetailPageData,

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
      this.setData({ order, statusLabel: getOrderStatusLabel(order.status) })
      wx.setNavigationBarTitle({ title: `${order.room_number} 房订单` })
    } catch (error) {
      this.setData({ errorMessage: error instanceof Error ? error.message : '订单详情读取失败' })
    } finally {
      this.setData({ loading: false })
    }
  },

  onOrdersTap() {
    wx.navigateBack({ delta: 1 })
  },

  onHomeTap() {
    wx.reLaunch({ url: '/pages/home/index' })
  },
})
