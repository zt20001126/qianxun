/** 小程序预订信息、T03 积分预览与 P01 十分钟锁房。 */
import { checkRoomAvailability, getRoomDetail } from '../../services/rooms'
import { createPrepay, previewPoints } from '../../services/booking'
import type { AppRoomDetail } from '../../types/room'
import type { AppAvailabilityCheck } from '../../types/room'
import type { PointsPreview, PrepayResult } from '../../types/booking'
import { isValidMainlandPhone } from '../../utils/phone'
import { addDays, daysBetween, getBookingDateWindow } from '../../utils/stay-date'

interface BookingPageData {
  room: AppRoomDetail | null
  roomId: number
  checkIn: string
  checkOut: string
  checkInLabel: string
  checkOutLabel: string
  checkInWeek: string
  checkOutWeek: string
  nights: number
  tierLabel: string
  roomPriceLabel: string
  roomAmountLabel: string
  discountAmountLabel: string
  payAmountLabel: string
  payButtonLabel: string
  availablePointsLabel: string
  pointsUsedLabel: string
  guestName: string
  phone: string
  guestCount: number
  remark: string
  preview: PointsPreview | null
  availability: AppAvailabilityCheck | null
  availabilityVerified: boolean
  availabilityError: string
  loading: boolean
  submitting: boolean
  previewLoading: boolean
  calendarVisible: boolean
  usePoints: boolean
  errorMessage: string
  ruleVisible: boolean
}

Page({
  data: {
    room: null,
    roomId: 0,
    checkIn: '',
    checkOut: '',
    checkInLabel: '',
    checkOutLabel: '',
    checkInWeek: '',
    checkOutWeek: '',
    nights: 0,
    tierLabel: '',
    roomPriceLabel: '',
    roomAmountLabel: '',
    discountAmountLabel: '',
    payAmountLabel: '',
    payButtonLabel: '立即支付 ¥0',
    availablePointsLabel: '0',
    pointsUsedLabel: '0',
    guestName: '',
    phone: '',
    guestCount: 1,
    remark: '',
    preview: null,
    availability: null,
    availabilityVerified: false,
    availabilityError: '',
    loading: true,
    submitting: false,
    previewLoading: false,
    calendarVisible: false,
    usePoints: false,
    errorMessage: '',
    ruleVisible: false,
  } as BookingPageData,

  async onLoad(options: Record<string, string | undefined>) {
    const roomId = Number(options.room_id)
    const dateWindow = getBookingDateWindow()
    const checkIn = options.check_in || dateWindow.today
    const checkOut = options.check_out || addDays(checkIn, 1)
    const validRange = Number.isInteger(roomId) && roomId > 0
      && checkIn >= dateWindow.today
      && checkIn <= dateWindow.lastCheckInDate
      && checkOut > checkIn
      && checkOut <= dateWindow.lastSelectableDate
    if (!validRange) {
      this.setData({ loading: false, errorMessage: '预订房间或日期无效，请返回重新选择' })
      return
    }

    this.setData({
      roomId,
      checkIn,
      checkOut,
      checkInLabel: formatMonthDay(checkIn),
      checkOutLabel: formatMonthDay(checkOut),
      checkInWeek: formatWeek(checkIn),
      checkOutWeek: formatWeek(checkOut),
      nights: daysBetween(checkIn, checkOut),
    })
    try {
      const room = await getRoomDetail(roomId)
      this.setData({ room, roomPriceLabel: formatPrice(room.price), guestCount: 1 })
      wx.setNavigationBarTitle({ title: '填写预订信息' })
      await this.refreshAvailability()
      const initialPreview = await this.loadPreview(0)
      if (initialPreview && initialPreview.max_points_usable > 0) {
        this.setData({ usePoints: true })
        const pointsPreview = await this.loadPreview(initialPreview.max_points_usable)
        if (!pointsPreview) this.setData({ usePoints: false })
      }
    } catch (error) {
      this.setData({ errorMessage: error instanceof Error ? error.message : '读取预订资料失败' })
    } finally {
      this.setData({ loading: false })
    }
  },

  onGuestNameInput(event: WechatMiniprogram.Input) {
    this.setData({ guestName: event.detail.value })
  },

  onPhoneInput(event: WechatMiniprogram.Input) {
    this.setData({ phone: event.detail.value })
  },

  onRemarkInput(event: WechatMiniprogram.Input) {
    this.setData({ remark: event.detail.value })
  },

  onGuestCountStep(event: WechatMiniprogram.TouchEvent) {
    const delta = Number(event.currentTarget.dataset.delta)
    const capacity = this.data.room?.capacity || 1
    const next = this.data.guestCount + delta
    if (next < 1 || next > capacity) {
      if (next > capacity) wx.showToast({ title: '已达到该房型最多入住人数', icon: 'none' })
      return
    }
    this.setData({ guestCount: next })
  },

  async onPointsToggle() {
    const currentPreview = this.data.preview
    if (this.data.previewLoading || !currentPreview || currentPreview.max_points_usable <= 0) return
    const usePoints = !this.data.usePoints
    const pointsUsed = usePoints ? currentPreview.max_points_usable : 0
    this.setData({ usePoints })
    const preview = await this.loadPreview(pointsUsed)
    if (!preview) this.setData({ usePoints: !usePoints })
  },

  async loadPreview(pointsUsed: number): Promise<PointsPreview | null> {
    if (!this.data.roomId) return null
    this.setData({ previewLoading: true, errorMessage: '' })
    try {
      const preview = await previewPoints({
        room_id: this.data.roomId,
        check_in: this.data.checkIn,
        check_out: this.data.checkOut,
        points_used: pointsUsed,
      })
      this.setData({
        preview,
        tierLabel: formatTierRateLabel(preview.points_per_yuan),
        roomAmountLabel: formatPrice(preview.room_amount),
        discountAmountLabel: formatPrice(preview.discount_amount),
        payAmountLabel: formatPrice(preview.pay_amount),
        payButtonLabel: '立即支付 ¥' + formatPrice(preview.pay_amount),
        availablePointsLabel: formatNumber(preview.available_points),
        pointsUsedLabel: formatNumber(preview.points_used),
      })
      return preview
    } catch (error) {
      this.setData({ errorMessage: error instanceof Error ? error.message : '积分试算失败' })
      return null
    } finally {
      this.setData({ previewLoading: false })
    }
  },

  async refreshAvailability(): Promise<boolean> {
    this.setData({ availability: null, availabilityVerified: false, availabilityError: '' })
    try {
      const availability = await checkRoomAvailability(this.data.roomId, this.data.checkIn, this.data.checkOut)
      this.setData({ availability, availabilityVerified: true })
      return availability.available
    } catch (error) {
      this.setData({
        availabilityError: error instanceof Error ? error.message : '日期复核失败',
      })
      return false
    }
  },

  onCalendarOpen() {
    this.setData({ calendarVisible: true })
  },

  onCalendarClose() {
    this.setData({ calendarVisible: false })
  },

  async onCalendarConfirm(event: WechatMiniprogram.CustomEvent<{ checkIn: string; checkOut: string }>) {
    const { checkIn, checkOut } = event.detail
    this.setData({
      calendarVisible: false,
      checkIn,
      checkOut,
      checkInLabel: formatMonthDay(checkIn),
      checkOutLabel: formatMonthDay(checkOut),
      checkInWeek: formatWeek(checkIn),
      checkOutWeek: formatWeek(checkOut),
      nights: daysBetween(checkIn, checkOut),
      roomAmountLabel: '',
      discountAmountLabel: '',
      payAmountLabel: '',
      preview: null,
      usePoints: false,
    })
    const available = await this.refreshAvailability()
    const initialPreview = await this.loadPreview(0)
    if (available && initialPreview && initialPreview.max_points_usable > 0) {
      this.setData({ usePoints: true })
      await this.loadPreview(initialPreview.max_points_usable)
    }
  },

  onPointsRuleOpen() {
    this.setData({ ruleVisible: true })
  },

  onPointsRuleClose() {
    this.setData({ ruleVisible: false })
  },

  async onPayTap() {
    if (this.data.submitting || this.data.loading || this.data.previewLoading) return
    const room = this.data.room
    const preview = this.data.preview
    if (!room || !preview) {
      wx.showToast({ title: '费用试算尚未完成', icon: 'none' })
      return
    }
    if (!this.data.guestName.trim()) {
      wx.showToast({ title: '请填写预订人姓名', icon: 'none' })
      return
    }
    if (!isValidMainlandPhone(this.data.phone.trim())) {
      wx.showToast({ title: '请填写正确的大陆手机号', icon: 'none' })
      return
    }
    if (this.data.guestCount < 1 || this.data.guestCount > room.capacity) {
      wx.showToast({ title: '入住人数超出该房型限制', icon: 'none' })
      return
    }

    this.setData({ submitting: true, errorMessage: '' })
    try {
      this.setData({ availabilityVerified: false, availability: null })
      const availability = await checkRoomAvailability(this.data.roomId, this.data.checkIn, this.data.checkOut)
      this.setData({ availability, availabilityVerified: true })
      if (!availability.available) {
        wx.showToast({ title: '所选日期已不可订，请调整行程', icon: 'none' })
        return
      }
      const prepay: PrepayResult = await createPrepay({
        room_id: this.data.roomId,
        check_in: this.data.checkIn,
        check_out: this.data.checkOut,
        guest_name: this.data.guestName.trim(),
        phone: this.data.phone.trim(),
        guest_count: this.data.guestCount,
        remark: this.data.remark.trim(),
        points_used: this.data.usePoints ? preview.points_used : 0,
      })
      wx.redirectTo({ url: buildPaymentUrl(prepay) })
    } catch (error) {
      this.setData({ errorMessage: error instanceof Error ? error.message : '创建预支付失败' })
      wx.showToast({ title: '创建预支付失败，请检查房态后重试', icon: 'none' })
    } finally {
      this.setData({ submitting: false })
    }
  },

  noop() {},
})

function formatMonthDay(date: string): string {
  const [, month, day] = date.split('-')
  return Number(month) + '月' + Number(day) + '日'
}

function formatWeek(date: string): string {
  const [year, month, day] = date.split('-').map(Number)
  return ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][
    new Date(Date.UTC(year, month - 1, day)).getUTCDay()
  ]
}

function formatTierRateLabel(pointsPerYuan: number): string {
  const tierNames: Record<number, string> = {
    30: '普通会员',
    25: '银卡会员',
    20: '金卡会员',
    15: '钻石会员',
  }
  const tierName = tierNames[pointsPerYuan]
  return tierName ? `${tierName}（${pointsPerYuan}积分=¥1）` : `${pointsPerYuan}积分=¥1`
}

function formatPrice(price: string): string {
  return price.replace(/\.00$/, '')
}

function formatNumber(value: number): string {
  return String(value).replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

function buildPaymentUrl(prepay: PrepayResult): string {
  const params = [
    'payment_no=' + encodeURIComponent(prepay.payment_no),
    'room_amount=' + encodeURIComponent(prepay.room_amount),
    'points_used=' + prepay.points_used,
    'discount_amount=' + encodeURIComponent(prepay.discount_amount),
    'pay_amount=' + encodeURIComponent(prepay.pay_amount),
    'expires_at=' + encodeURIComponent(prepay.expires_at),
  ]
  return '/pages/payment/index?' + params.join('&')
}
