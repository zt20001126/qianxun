/** 当前会员商城订单列表，沿用房间订单的卡片层级并跳转至详情页操作。 */
import { listMallOrders, type MallOrder } from '../../services/mall'

const statusLabels: Record<MallOrder['status'], string> = {
  PENDING_SHIPMENT: '待发货',
  SHIPPED: '已发货',
  COMPLETED: '已完成',
  REFUNDED: '已退款',
}

interface DisplayOrder extends MallOrder {
  status_label: string
  paid_amount_display: string
  shipping_label: string
}

interface MallOrdersPageData {
  orders: DisplayOrder[]
  loading: boolean
  loadingMore: boolean
  errorMessage: string
  page: number
  total: number
  hasMore: boolean
}

function formatMoney(value: string): string {
  return value.endsWith('.00') ? value.slice(0, -3) : value
}

function toDisplayOrder(order: MallOrder): DisplayOrder {
  const shippingLabel = order.tracking_no
    ? `${order.express_company || '快递'}：${order.tracking_no}`
    : order.status === 'PENDING_SHIPMENT' ? '商家正在备货，请耐心等待' : '暂无物流信息'
  return {
    ...order,
    status_label: statusLabels[order.status],
    paid_amount_display: formatMoney(order.paid_amount),
    shipping_label: shippingLabel,
  }
}

Page({
  data: {
    orders: [],
    loading: true,
    loadingMore: false,
    errorMessage: '',
    page: 1,
    total: 0,
    hasMore: false,
  } as MallOrdersPageData,

  onLoad() {
    wx.setNavigationBarTitle({ title: '商城订单' })
    void this.loadOrders()
  },

  onShow() {
    if (!this.data.loading) void this.loadOrders()
  },

  async onPullDownRefresh() {
    await this.loadOrders()
    wx.stopPullDownRefresh()
  },

  async onReachBottom() {
    if (this.data.loading || this.data.loadingMore || !this.data.hasMore) return
    this.setData({ loadingMore: true })
    try {
      const nextPage = this.data.page + 1
      const result = await listMallOrders(nextPage)
      this.setData({
        orders: [...this.data.orders, ...result.list.map(toDisplayOrder)],
        page: result.page,
        total: result.total,
        hasMore: result.page * result.size < result.total,
      })
    } catch (error) {
      wx.showToast({ title: error instanceof Error ? error.message : '更多订单读取失败', icon: 'none' })
    } finally {
      this.setData({ loadingMore: false })
    }
  },

  async loadOrders() {
    this.setData({
      orders: [],
      loading: true,
      loadingMore: false,
      errorMessage: '',
      page: 1,
      total: 0,
      hasMore: false,
    })
    try {
      const result = await listMallOrders(1)
      this.setData({
        orders: result.list.map(toDisplayOrder),
        page: result.page,
        total: result.total,
        hasMore: result.page * result.size < result.total,
      })
    } catch (error) {
      this.setData({ errorMessage: error instanceof Error ? error.message : '订单读取失败' })
    } finally {
      this.setData({ loading: false })
    }
  },

  onRetry() { void this.loadOrders() },
  onOrderTap(event: WechatMiniprogram.TouchEvent) {
    const id = Number(event.currentTarget.dataset.orderId)
    if (Number.isInteger(id) && id > 0) {
      wx.navigateTo({ url: `/pages/mall-order-detail/index?id=${id}` })
    }
  },
})
