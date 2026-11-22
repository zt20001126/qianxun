/** 当前会员查看商城订单详情，并在符合状态时申请整单退款。 */
import { getMallOrder, refundMallOrder, type MallOrderDetail } from '../../services/mall'
import { STORE_CONTACT_PHONE_LABEL } from '../../config'
import { callBusinessPhone } from '../../utils/business-contact'

const statusLabels: Record<MallOrderDetail['status'], string> = {
  PENDING_SHIPMENT: '待发货',
  SHIPPED: '已发货',
  COMPLETED: '已完成',
  REFUNDED: '已退款',
}

interface DetailRow {
  label: string
  value: string
  mono?: boolean
  strong?: boolean
}

interface MallOrderDetailPageData {
  order: MallOrderDetail | null
  detailRows: DetailRow[]
  statusLabel: string
  statusHint: string
  totalAmountLabel: string
  discountAmountLabel: string
  paidAmountLabel: string
  loading: boolean
  errorMessage: string
  refunding: boolean
  contactPhone: string
}

/** 将服务端金额转为页面使用的无货币符号格式，避免浮点金额计算。 */
function formatMoney(value: string | null | undefined): string {
  const cents = toCents(value)
  if (cents === null) return '—'
  return cents % 100 === 0 ? String(cents / 100) : `${Math.floor(cents / 100)}.${String(cents % 100).padStart(2, '0')}`
}

/** 把两位小数金额转换为分；商城金额上限远低于 JavaScript 安全整数范围。 */
function toCents(value: string | null | undefined): number | null {
  if (typeof value !== 'string') return null
  const matched = /^(\d+)(?:\.(\d{1,2}))?$/.exec(value)
  if (!matched) return null
  const whole = Number(matched[1])
  if (!Number.isSafeInteger(whole)) return null
  return whole * 100 + Number((matched[2] || '').padEnd(2, '0'))
}

/** 使用分级整数相减，展示积分抵扣金额。 */
function resolveDiscount(totalAmount: string, paidAmount: string): string {
  const totalCents = toCents(totalAmount)
  const paidCents = toCents(paidAmount)
  if (totalCents === null || paidCents === null || paidCents > totalCents) return '—'
  const discountCents = totalCents - paidCents
  return discountCents % 100 === 0
    ? String(discountCents / 100)
    : `${Math.floor(discountCents / 100)}.${String(discountCents % 100).padStart(2, '0')}`
}

function formatDateTime(value: string | null): string {
  if (!value) return '—'
  const matched = /^(\d{4}-\d{2}-\d{2})[T ](\d{2}:\d{2})/.exec(value)
  return matched ? `${matched[1]} ${matched[2]}` : value
}

function getStatusHint(order: MallOrderDetail): string {
  if (order.status === 'PENDING_SHIPMENT') return '订单已支付，商家正在备货，请耐心等待。'
  if (order.status === 'SHIPPED') return '商品已发出，请留意快递物流信息。'
  if (order.status === 'COMPLETED') return '订单已完成，感谢您选择芊浔好物。'
  return '订单已退款，已使用积分会按退款结果返还。'
}

function buildDetailRows(order: MallOrderDetail): DetailRow[] {
  const rows: DetailRow[] = [
    { label: '订单号', value: order.order_no, mono: true },
    { label: '商品名称', value: order.product_name, strong: true },
    { label: '购买数量', value: `${order.quantity}件` },
    { label: '收货人', value: order.recipient_name || '—' },
    { label: '手机号码', value: order.recipient_phone || '—' },
    { label: '收货地址', value: order.shipping_address || '—' },
    { label: '支付时间', value: formatDateTime(order.paid_at || order.created_at) },
  ]
  if (order.express_company || order.tracking_no || order.shipped_at) {
    rows.push(
      { label: '快递公司', value: order.express_company || '—' },
      { label: '快递单号', value: order.tracking_no || '—', mono: Boolean(order.tracking_no) },
      { label: '发货时间', value: formatDateTime(order.shipped_at) },
    )
  }
  if (order.completed_at) rows.push({ label: '完成时间', value: formatDateTime(order.completed_at) })
  return rows
}

Page({
  data: {
    order: null,
    detailRows: [],
    statusLabel: '',
    statusHint: '',
    totalAmountLabel: '—',
    discountAmountLabel: '—',
    paidAmountLabel: '—',
    loading: true,
    errorMessage: '',
    refunding: false,
    contactPhone: STORE_CONTACT_PHONE_LABEL,
  } as MallOrderDetailPageData,

  orderId: 0,

  async onLoad(options: Record<string, string | undefined>) {
    const orderId = Number(options.id)
    if (!Number.isInteger(orderId) || orderId <= 0) {
      this.setData({ loading: false, errorMessage: '商城订单编号无效' })
      return
    }
    this.orderId = orderId
    await this.loadOrder()
  },

  async onPullDownRefresh() {
    await this.loadOrder()
    wx.stopPullDownRefresh()
  },

  /** 从服务端读取当前会员自己的商城订单，避免以列表缓存替代订单快照。 */
  async loadOrder() {
    if (!this.orderId) return
    this.setData({ loading: true, errorMessage: '' })
    try {
      const order = await getMallOrder(this.orderId)
      this.setData({
        order,
        detailRows: buildDetailRows(order),
        statusLabel: statusLabels[order.status],
        statusHint: getStatusHint(order),
        totalAmountLabel: formatMoney(order.total_amount),
        discountAmountLabel: resolveDiscount(order.total_amount, order.paid_amount),
        paidAmountLabel: formatMoney(order.paid_amount),
      })
      wx.setNavigationBarTitle({ title: '商城订单详情' })
    } catch (error) {
      this.setData({ errorMessage: error instanceof Error ? error.message : '商城订单详情读取失败' })
    } finally {
      this.setData({ loading: false })
    }
  },

  onOrdersTap() {
    wx.redirectTo({ url: '/pages/mall-orders/index' })
  },

  /** 待发货订单可退款；其他状态保留联系商家的明确入口。 */
  onCancelTap() {
    const order = this.data.order
    if (!order || order.status === 'REFUNDED' || this.data.refunding) return
    if (order.status !== 'PENDING_SHIPMENT') {
      wx.showModal({
        title: '暂不支持自助取消',
        content: '当前商城订单已发货或已完成，如需售后请联系商家。',
        cancelText: '我知道了',
        confirmText: '联系商家',
        success: (result) => {
          if (result.confirm) callBusinessPhone()
        },
      })
      return
    }
    wx.showModal({
      title: '确认取消订单？',
      content: `取消后将退回实付金额 ¥${this.data.paidAmountLabel}${order.points_used > 0 ? `，并返还 ${order.points_used} 积分` : ''}。`,
      cancelText: '再想想',
      confirmText: '确认取消',
      confirmColor: '#B83B2A',
      success: (result) => {
        if (result.confirm) void this.submitRefund()
      },
    })
  },

  async submitRefund() {
    const order = this.data.order
    if (!order || order.status !== 'PENDING_SHIPMENT' || this.data.refunding) return
    this.setData({ refunding: true })
    try {
      const result = await refundMallOrder(order.id)
      const success = result.status === 'SUCCESS'
      wx.showToast({ title: success ? '退款成功，订单已取消' : (result.status === 'PENDING' ? '退款处理中' : '退款未完成'), icon: success ? 'success' : 'none' })
      await this.loadOrder()
    } catch (error) {
      wx.showToast({ title: error instanceof Error ? error.message : '退款未完成，请稍后重试', icon: 'none' })
      await this.loadOrder()
    } finally {
      this.setData({ refunding: false })
    }
  },

  onContactTap() {
    wx.showModal({
      title: '联系芊浔驻店管家',
      content: `商家电话：${this.data.contactPhone}\n服务时间：07:30 - 23:30`,
      cancelText: '取消',
      confirmText: '立即拨打',
      success: (result) => {
        if (result.confirm) callBusinessPhone()
      },
    })
  },
})
