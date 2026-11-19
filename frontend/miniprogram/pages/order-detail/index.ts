/** O02 小程序本人订单详情与 O03/P03 自助退款交互。 */
import { getRoomDetail } from '../../services/rooms'
import { getCancellationEligibility, getMyOrder, refundMyOrder } from '../../services/orders'
import type { AppOrderDetail, OrderCancellationEligibility } from '../../types/booking'
import { getOrderStatusLabel } from '../../types/booking'
import { STORE_CONTACT_PHONE } from '../../config'
import { callBusinessPhone } from '../../utils/business-contact'

type DialogType = 'none' | 'unavailable' | 'refund' | 'contact'

interface DetailRow {
  label: string
  value: string
  isMono?: boolean
  isStrong?: boolean
}

interface OrderDetailPageData {
  order: AppOrderDetail | null
  statusLabel: string
  statusHint: string
  detailRows: DetailRow[]
  cancellation: OrderCancellationEligibility | null
  loading: boolean
  errorMessage: string
  dialogType: DialogType
  refundSubmitting: boolean
  contactPhone: string
  cancelDialogMessage: string
  unitPrice: string
  roomAmountLabel: string
  discountAmountLabel: string
  paidAmountLabel: string
}

function formatDateWithWeekday(value: string): string {
  const [year, month, day] = value.split('-').map(Number)
  const weekday = ['日', '一', '二', '三', '四', '五', '六'][new Date(Date.UTC(year, month - 1, day)).getUTCDay()]
  return `${value}（周${weekday}）`
}

function formatMoney(value: string): string {
  return value.endsWith('.00') ? value.slice(0, -3) : value
}

/** 按分做字符串长除法，避免用浮点数计算页面展示金额。 */
function divideMoneyByNights(roomAmount: string, nights: number): string {
  if (nights <= 0) return '0.00'
  const [whole, fraction = ''] = roomAmount.split('.')
  const cents = `${whole}${fraction.padEnd(2, '0').slice(0, 2)}`.replace(/^0+(?=\d)/, '')
  let remainder = 0
  let quotient = ''

  for (const digit of cents) {
    const current = remainder * 10 + Number(digit)
    quotient += String(Math.floor(current / nights))
    remainder = current % nights
  }

  // 单店房价按房间固定夜价保存，房费总额应可被住宿晚数整除。
  if (remainder !== 0) return '—'
  const padded = quotient.padStart(3, '0')
  return `${padded.slice(0, -2)}.${padded.slice(-2)}`
}

function getStatusHint(
  order: AppOrderDetail,
  cancellation: OrderCancellationEligibility | null,
): string {
  if (order.status === 'CANCELLED') return '订单已取消，房费及已使用积分已全额退还。'
  if (cancellation?.eligible) return '可在入住当天 18:00（北京时间）前自助申请取消退款。'
  if (cancellation?.reason) return cancellation.reason
  return '当前订单不支持自助退款，如需退改请联系商家协商。'
}

function formatContactPhone(phone: string): string {
  return `${phone.slice(0, 4)}-${phone.slice(4, 8)} ${phone.slice(8)}`
}

Page({
  data: {
    order: null,
    statusLabel: '',
    statusHint: '',
    detailRows: [],
    cancellation: null,
    loading: true,
    errorMessage: '',
    dialogType: 'none',
    refundSubmitting: false,
    contactPhone: formatContactPhone(STORE_CONTACT_PHONE),
    cancelDialogMessage:
      '当前订单不支持自助退款（仅待确认/待入住且早于入住当日 18:00 可自助取消），如需退改请联系商家协商。',
    unitPrice: '0.00',
    roomAmountLabel: '0',
    discountAmountLabel: '0',
    paidAmountLabel: '0',
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
      const [room, cancellation] = await Promise.all([
        getRoomDetail(order.room_id).catch(() => null),
        order.status === 'CANCELLED'
          ? Promise.resolve(null)
          : getCancellationEligibility(id).catch(() => ({
              eligible: false,
              reason: '暂时无法读取取消资格，请稍后重试或联系商家',
              deadline: '',
            })),
      ])
      const roomName = room ? `${order.room_number} · ${room.name}` : `${order.room_number} 房`
      const detailRows: DetailRow[] = [
        { label: '订单号', value: order.order_no, isMono: true },
        { label: '房间', value: roomName, isStrong: true },
        { label: '入住日期', value: formatDateWithWeekday(order.check_in) },
        { label: '退房日期', value: formatDateWithWeekday(order.check_out) },
        { label: '住宿晚数', value: `${order.nights}晚` },
        { label: '入住人数', value: order.guest_count == null ? '—' : `${order.guest_count}人` },
        { label: '预订人', value: order.guest_name || '—' },
        { label: '手机号码', value: order.phone || '—' },
        { label: '备注', value: order.remark || '—' },
      ]
      this.setData({
        order,
        detailRows,
        cancellation,
        statusLabel: getOrderStatusLabel(order.status),
        statusHint: getStatusHint(order, cancellation),
        unitPrice: divideMoneyByNights(order.room_amount, order.nights),
        roomAmountLabel: formatMoney(order.room_amount),
        discountAmountLabel: formatMoney(order.discount_amount),
        paidAmountLabel: formatMoney(order.paid_amount),
      })
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

  onCancelTap() {
    const { order, cancellation } = this.data
    if (!order || order.status === 'CANCELLED' || this.data.refundSubmitting) return
    this.setData({ dialogType: cancellation?.eligible ? 'refund' : 'unavailable' })
  },

  onContactTap() {
    this.setData({ dialogType: 'contact' })
  },

  onDialogClose() {
    if (!this.data.refundSubmitting) this.setData({ dialogType: 'none' })
  },

  onDialogNoop() {},

  onDialogPhoneTap() {
    callBusinessPhone()
  },

  async onRefundConfirm() {
    const order = this.data.order
    if (!order || !this.data.cancellation?.eligible || this.data.refundSubmitting) return
    this.setData({ refundSubmitting: true })
    try {
      await refundMyOrder(order.id)
      this.setData({ dialogType: 'none' })
      wx.showToast({ title: '订单已取消', icon: 'success' })
      await this.loadOrder(order.id)
    } catch (error) {
      this.setData({ dialogType: 'none' })
      wx.showToast({ title: error instanceof Error ? error.message : '退款未完成，请稍后重试', icon: 'none' })
      await this.loadOrder(order.id)
    } finally {
      this.setData({ refundSubmitting: false })
    }
  },
})
