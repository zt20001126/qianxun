/** 当前会员的商城订单列表。 */
import { listMallOrders, refundMallOrder, type MallOrder } from '../../services/mall'

type DisplayOrder = MallOrder & { status_label: string }
const statusLabels: Record<MallOrder['status'], string> = {
  PENDING_SHIPMENT: '待发货', SHIPPED: '已发货', COMPLETED: '已完成', REFUNDED: '已退款',
}
Page({
  data: { orders: [] as DisplayOrder[], loading: false, refundingId: 0, error: '', page: 1, total: 0, hasMore: false },
  onShow() { void this.loadOrders(true) },
  async onPullDownRefresh() { await this.loadOrders(true); wx.stopPullDownRefresh() },
  onReachBottom() { if (this.data.hasMore) void this.loadOrders(false) },
  async loadOrders(reset = true) {
    if (this.data.loading) return
    this.setData({ loading: true, error: '' })
    try {
      const result = await listMallOrders(reset ? 1 : this.data.page + 1)
      const incoming = result.list.map((order) => ({ ...order, status_label: statusLabels[order.status] || order.status }))
      this.setData({ orders: reset ? incoming : [...this.data.orders, ...incoming], page: result.page,
        total: result.total, hasMore: result.page * result.size < result.total })
    } catch (error) { this.setData({ error: error instanceof Error ? error.message : '订单加载失败' }) }
    finally { this.setData({ loading: false }) }
  },
  onRetry() { void this.loadOrders(true) },
  onRefund(event: WechatMiniprogram.TouchEvent) {
    const id = Number(event.currentTarget.dataset.id)
    if (!Number.isInteger(id) || id <= 0 || this.data.refundingId) return
    wx.showModal({ title: '申请退款', content: '确认对这笔待发货订单申请整单退款吗？', success: (result) => {
      if (result.confirm) void this.submitRefund(id)
    } })
  },
  async submitRefund(id: number) {
    if (this.data.refundingId) return
    this.setData({ refundingId: id })
    try {
      const result = await refundMallOrder(id)
      wx.showToast({ title: result.status === 'SUCCESS' ? '退款成功' : (result.status === 'PENDING' ? '退款处理中' : '退款未成功'),
        icon: result.status === 'SUCCESS' ? 'success' : 'none' })
      await this.loadOrders(true)
    } catch (error) { wx.showToast({ title: error instanceof Error ? error.message : '退款失败', icon: 'none' }) }
    finally { this.setData({ refundingId: 0 }) }
  },
})
