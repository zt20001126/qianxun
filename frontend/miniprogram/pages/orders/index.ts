/** O01 小程序本人正式订单列表。 */
import { listMyOrders } from '../../services/orders'
import { listRooms } from '../../services/rooms'
import type { AppOrder } from '../../types/booking'
import type { AppRoomSummary } from '../../types/room'
import { getOrderStatusLabel } from '../../types/booking'

const PAGE_SIZE = 20

interface DisplayOrder extends AppOrder {
  status_label: string
  room_name: string
  cover_image: string
  check_in_label: string
  check_out_label: string
  paid_amount_display: string
}

interface OrdersPageData {
  orders: DisplayOrder[]
  rooms: AppRoomSummary[]
  loading: boolean
  loadingMore: boolean
  errorMessage: string
  total: number
  page: number
  hasMore: boolean
}

function formatMonthDay(value: string): string {
  const month = Number(value.slice(5, 7))
  const day = Number(value.slice(8, 10))
  return `${month}月${day}日`
}

function formatMoney(value: string): string {
  return value.endsWith('.00') ? value.slice(0, -3) : value
}

function mapOrder(order: AppOrder, rooms: AppRoomSummary[]): DisplayOrder {
  const room = rooms.find((item) => item.id === order.room_id)
  return {
    ...order,
    status_label: getOrderStatusLabel(order.status),
    room_name: room ? `${order.room_number} · ${room.name}` : `${order.room_number} 房`,
    cover_image: room?.images[0] || '',
    check_in_label: formatMonthDay(order.check_in),
    check_out_label: formatMonthDay(order.check_out),
    paid_amount_display: formatMoney(order.paid_amount),
  }
}

Page({
  data: {
    orders: [],
    rooms: [],
    loading: true,
    loadingMore: false,
    errorMessage: '',
    total: 0,
    page: 1,
    hasMore: false,
  } as OrdersPageData,

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

  async onReachBottom() {
    if (this.data.loading || this.data.loadingMore || !this.data.hasMore) return
    this.setData({ loadingMore: true })
    try {
      const nextPage = this.data.page + 1
      const result = await listMyOrders(nextPage, PAGE_SIZE)
      const nextOrders = result.list.map((order) => mapOrder(order, this.data.rooms))
      this.setData({
        orders: [...this.data.orders, ...nextOrders],
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
      loading: true,
      loadingMore: false,
      errorMessage: '',
      orders: [],
      total: 0,
      page: 1,
      hasMore: false,
    })
    try {
      const [result, rooms] = await Promise.all([
        listMyOrders(1, PAGE_SIZE),
        listRooms().catch(() => [] as AppRoomSummary[]),
      ])
      this.setData({
        rooms,
        orders: result.list.map((order) => mapOrder(order, rooms)),
        total: result.total,
        page: result.page,
        hasMore: result.page * result.size < result.total,
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
})
