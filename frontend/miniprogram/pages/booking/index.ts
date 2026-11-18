/** 预订填写页：展示服务端试算并提交 P01 创建十分钟临时锁房。 */
import { createPrepay, previewPoints } from '../../services/booking'
import { getRoomDetail } from '../../services/rooms'
import type { AppRoomDetail } from '../../types/room'
import type { PointsPreview } from '../../types/booking'
import { isValidMainlandPhone } from '../../utils/phone'
import { daysBetween } from '../../utils/stay-date'

interface BookingPageData {
  room: AppRoomDetail | null
  roomId: number
  checkIn: string
  checkOut: string
  nights: number
  guestName: string
  phone: string
  guestCount: string
  guestCountOptions: string[]
  remark: string
  pointsUsedInput: string
  preview: PointsPreview | null
  loading: boolean
  submitting: boolean
  errorMessage: string
}

Page({
  data: {
    room: null,
    roomId: 0,
    checkIn: '',
    checkOut: '',
    nights: 0,
    guestName: '',
    phone: '',
    guestCount: '1',
    guestCountOptions: ['1'],
    remark: '',
    pointsUsedInput: '0',
    preview: null,
    loading: true,
    submitting: false,
    errorMessage: '',
  } as BookingPageData,

  async onLoad(options: Record<string, string | undefined>) {
    const roomId = Number(options.room_id)
    const checkIn = options.check_in || ''
    const checkOut = options.check_out || ''
    if (!Number.isInteger(roomId) || roomId <= 0 || daysBetween(checkIn, checkOut) <= 0) {
      this.setData({ loading: false, errorMessage: '预订房间或日期无效，请返回重新选择' })
      return
    }
    this.setData({ roomId, checkIn, checkOut, nights: daysBetween(checkIn, checkOut) })
    try {
      const room = await getRoomDetail(roomId)
      this.setData({ room, guestCount: '1', guestCountOptions: Array.from({ length: room.capacity }, (_, index) => String(index + 1)) })
      wx.setNavigationBarTitle({ title: '填写预订' })
      await this.loadPreview(0)
    } catch (error) {
      this.setData({ errorMessage: error instanceof Error ? error.message : '读取预订资料失败' })
    } finally {
      this.setData({ loading: false })
    }
  },

  onGuestNameInput(event: WechatMiniprogram.Input) {
    this.setData({ guestName: event.detail.value.trim() })
  },

  onPhoneInput(event: WechatMiniprogram.Input) {
    this.setData({ phone: event.detail.value.trim() })
  },

  onGuestCountChange(event: WechatMiniprogram.PickerChange) {
    this.setData({ guestCount: String(Number(event.detail.value) + 1) })
  },

  onRemarkInput(event: WechatMiniprogram.Input) {
    this.setData({ remark: event.detail.value })
  },

  onPointsInput(event: WechatMiniprogram.Input) {
    this.setData({ pointsUsedInput: event.detail.value.replace(/\D/g, '') })
  },

  async onPreviewTap() {
    await this.loadPreview(this.pointsUsed())
  },

  async loadPreview(pointsUsed: number) {
    this.setData({ errorMessage: '' })
    try {
      const preview = await previewPoints({
        room_id: this.data.roomId,
        check_in: this.data.checkIn,
        check_out: this.data.checkOut,
        points_used: pointsUsed,
      })
      this.setData({ preview })
    } catch (error) {
      this.setData({ errorMessage: error instanceof Error ? error.message : '积分试算失败' })
    }
  },

  async onSubmitTap() {
    if (this.data.submitting || !this.data.room) return
    const guestName = this.data.guestName.trim()
    const phone = this.data.phone.trim()
    const guestCount = Number(this.data.guestCount)
    if (!guestName) {
      wx.showToast({ title: '请填写预订联系人', icon: 'none' })
      return
    }
    if (!isValidMainlandPhone(phone)) {
      wx.showToast({ title: '请填写正确的大陆手机号', icon: 'none' })
      return
    }
    if (guestCount < 1 || guestCount > this.data.room.capacity) {
      wx.showToast({ title: `入住人数须为 1 至 ${this.data.room.capacity} 人`, icon: 'none' })
      return
    }

    this.setData({ submitting: true })
    try {
      const prepay = await createPrepay({
        room_id: this.data.roomId,
        check_in: this.data.checkIn,
        check_out: this.data.checkOut,
        guest_name: guestName,
        phone,
        guest_count: guestCount,
        remark: this.data.remark.trim(),
        points_used: this.pointsUsed(),
      })
      const query = [
        `payment_no=${encodeURIComponent(prepay.payment_no)}`,
        `room_amount=${encodeURIComponent(prepay.room_amount)}`,
        `points_used=${prepay.points_used}`,
        `discount_amount=${encodeURIComponent(prepay.discount_amount)}`,
        `pay_amount=${encodeURIComponent(prepay.pay_amount)}`,
        `expires_at=${encodeURIComponent(prepay.expires_at)}`,
      ].join('&')
      wx.redirectTo({ url: `/pages/payment/index?${query}` })
    } catch (error) {
      wx.showToast({ title: error instanceof Error ? error.message : '创建预支付失败', icon: 'none' })
    } finally {
      this.setData({ submitting: false })
    }
  },

  pointsUsed(): number {
    const value = Number(this.data.pointsUsedInput || '0')
    return Number.isSafeInteger(value) && value >= 0 ? value : 0
  },
})
