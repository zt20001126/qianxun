/** O02 小程序本人订单详情与 O03/P03 自助退款交互。 */
import { getRoomDetail } from '../../services/rooms'
import { getCancellationEligibility, getLatestRefund, getMyOrder, refundMyOrder } from '../../services/orders'
import type { AppOrderDetail, OrderCancellationEligibility } from '../../types/booking'
import { getOrderStatusLabel } from '../../types/booking'
import { STORE_CONTACT_PHONE_LABEL } from '../../config'
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
  refundPending: boolean
  contactPhone: string
  cancelDialogMessage: string
  unitPrice: string
  roomAmountLabel: string
  discountAmountLabel: string
  paidAmountLabel: string
}

function formatDateWithWeekday(value?: string | null): string {
  if (typeof value !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(value)) return '—'
  const [year, month, day] = value.split('-').map(Number)
  const weekday = ['日', '一', '二', '三', '四', '五', '六'][new Date(Date.UTC(year, month - 1, day)).getUTCDay()]
  return `${value}（周${weekday}）`
}

function normalizeMoney(value?: string | null): string | null {
  if (typeof value !== 'string' || !/^\d+(?:\.\d{1,2})?$/.test(value)) return null
  const [whole, fraction = ''] = value.split('.')
  return `${whole.replace(/^0+(?=\d)/, '')}.${fraction.padEnd(2, '0')}`
}

function formatMoney(value?: string | null): string {
  const normalized = normalizeMoney(value)
  if (!normalized) return '—'
  return normalized.endsWith('.00') ? normalized.slice(0, -3) : normalized
}

/** 使用契约中的等式兼容缺少 room_amount 的旧版详情响应。 */
function resolveRoomAmount(order: AppOrderDetail): string | null {
  const roomAmount = normalizeMoney(order.room_amount)
  if (roomAmount) return roomAmount

  const paidAmount = normalizeMoney(order.paid_amount)
  const discountAmount = normalizeMoney(order.discount_amount)
  if (paidAmount && discountAmount) return addMoneyStrings(paidAmount, discountAmount)
  if (paidAmount && Number(order.points_used || 0) === 0) return paidAmount
  return null
}

/** 按十进制字符串逐位相加，避免用浮点数处理金额。 */
function addMoneyStrings(left: string, right: string): string {
  const [leftWhole, leftFraction = ''] = left.split('.')
  const [rightWhole, rightFraction = ''] = right.split('.')
  const fractionSum = Number(leftFraction) + Number(rightFraction)
  let whole = addIntegerStrings(leftWhole, rightWhole)
  if (fractionSum >= 100) whole = addIntegerStrings(whole, '1')
  return `${whole}.${String(fractionSum % 100).padStart(2, '0')}`
}

function addIntegerStrings(left: string, right: string): string {
  let leftIndex = left.length - 1
  let rightIndex = right.length - 1
  let carry = 0
  let result = ''
  while (leftIndex >= 0 || rightIndex >= 0 || carry > 0) {
    const sum = Number(left[leftIndex] || 0) + Number(right[rightIndex] || 0) + carry
    result = String(sum % 10) + result
    carry = Math.floor(sum / 10)
    leftIndex -= 1
    rightIndex -= 1
  }
  return result.replace(/^0+(?=\d)/, '')
}

function divideMoneyByNights(roomAmount: string | null, nights: number): string {
  const normalizedAmount = normalizeMoney(roomAmount)
  if (!normalizedAmount || !Number.isInteger(nights) || nights <= 0) return '—'
  const [whole, fraction = ''] = normalizedAmount.split('.')
  const cents = `${whole}${fraction}`.replace(/^0+(?=\d)/, '')
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
  refundPending: boolean,
): string {
  if (refundPending) return '退款正在由支付平台处理中，完成后订单将自动取消。'
  if (order.status === 'CANCELLED') return '订单已取消，房费及已使用积分已全额退还。'
  if (cancellation?.eligible) return '可在入住当天 18:00（北京时间）前自助申请取消退款。'
  if (cancellation?.reason) return cancellation.reason
  return '当前订单不支持自助退款，如需退改请联系商家协商。'
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
    refundPending: false,
    contactPhone: STORE_CONTACT_PHONE_LABEL,
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
      const [room, cancellation, latestRefund] = await Promise.all([
        getRoomDetail(order.room_id).catch(() => null),
        order.status === 'CANCELLED'
          ? Promise.resolve(null)
          : getCancellationEligibility(id).catch(() => ({
              eligible: false,
              reason: '暂时无法读取取消资格，请稍后重试或联系商家',
              deadline: '',
            })),
        getLatestRefund(id).catch(() => null),
      ])
      const refundPending = latestRefund?.status === 'PENDING'
      const roomName = room ? `${order.room_number} · ${room.name}` : `${order.room_number} 房`
      const roomAmount = resolveRoomAmount(order)
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
        statusHint: getStatusHint(order, cancellation, refundPending),
        refundPending,
        unitPrice: divideMoneyByNights(roomAmount, order.nights),
        roomAmountLabel: roomAmount ? formatMoney(roomAmount) : '—',
        discountAmountLabel: formatMoney(order.discount_amount),
        paidAmountLabel: formatMoney(order.paid_amount),
      })
      wx.setNavigationBarTitle({ title: '订单详情' })
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
      const result = await refundMyOrder(order.id)
      this.setData({ dialogType: 'none' })
      if (result.status === 'SUCCESS') {
        wx.showToast({ title: '退款成功，订单已取消', icon: 'success' })
        await this.loadOrder(order.id)
      } else if (result.status === 'FAILED') {
        wx.showToast({ title: '退款失败，请联系商家处理', icon: 'none' })
      } else {
        this.setData({
          refundPending: true,
          statusHint: '退款正在由支付平台处理中，完成后订单将自动取消。',
        })
        wx.showToast({ title: '退款处理中', icon: 'none' })
        await this.pollRefundStatus(order.id)
      }
    } catch (error) {
      this.setData({ dialogType: 'none' })
      wx.showToast({ title: error instanceof Error ? error.message : '退款未完成，请稍后重试', icon: 'none' })
      await this.loadOrder(order.id)
    } finally {
      this.setData({ refundSubmitting: false })
    }
  },

  async pollRefundStatus(orderId: number) {
    for (let attempt = 0; attempt < 10; attempt += 1) {
      await new Promise((resolve) => setTimeout(resolve, 2000))
      try {
        const result = await getLatestRefund(orderId)
        if (result?.status === 'SUCCESS') {
          wx.showToast({ title: '退款成功，订单已取消', icon: 'success' })
          await this.loadOrder(orderId)
          return
        }
        if (result?.status === 'FAILED') {
          this.setData({ refundPending: false })
          wx.showToast({ title: '退款失败，请联系商家处理', icon: 'none' })
          await this.loadOrder(orderId)
          return
        }
      } catch {
        // 查询暂时失败时保留处理中提示，可由用户下拉刷新再次确认。
      }
    }
  },
})
