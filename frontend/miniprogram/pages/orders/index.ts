/** O01 小程序本人正式订单列表。 */
import { listMyOrders } from '../../services/orders'
import type { AppOrder, OrderStatus } from '../../types/booking'
import { getOrderStatusLabel } from '../../types/booking'

interface DisplayOrder extends AppOrder {
  status_label: string
}

interface OrdersPageData {
  orders: DisplayOrder[]
  loading: boolean
  errorMessage: string
  total: number
}

Page({
  data: { orders: [], loading: true, errorMessage: '', total: 0 } as OrdersPageData,

  onLoad() {
    wx.setNavigationBarTitle({ title: '我的订单' })
    void this.loadOrders()
  },

  onShow() {
    if (!this.data.loading) void this.loadOrders()
  },

  async onPullDownRefresh() {
    await this.loadOrders()
    wx.stopPullDownRefresh()
  },

  async loadOrders() {
    this.setData({ loading: true, errorMessage: '' })
    try {
      const result = await listMyOrders()
      this.setData({
        orders: result.list.map((order) => ({ ...order, status_label: getOrderStatusLabel(order.status as OrderStatus) })),
        total: result.total,
      })
    } catch (error) {
      this.setData({ errorMessage: error instanceof Error ? error.message : '订单读取失败' })
    } finally {
      this.setData({ loading: false })
    }
  },

  onOrderTap(event: WechatMiniprogram.TouchEvent) {
    const id = Number(event.currentTarget.dataset.orderId)
    if (Number.isInteger(id) && id > 0) wx.navigateTo({ url: `/pages/order-detail/index?id=${id}` })
  },

  onHomeTap() {
    wx.reLaunch({ url: '/pages/home/index' })
  },
})
