/** R02 房间详情、R03 日历入口与 R04 整段行程复核。 */
import { checkRoomAvailability, getRoomDetail } from '../../services/rooms'
import type { AppAvailabilityCheck, AppRoomDetail } from '../../types/room'
import { addDays, daysBetween, getBookingDateWindow } from '../../utils/stay-date'

interface RoomDetailPageData {
  room: AppRoomDetail | null
  facilities: Array<{ label: string; icon: string }>
  loading: boolean
  errorMessage: string
  imageIndex: number
  imageCount: number
  checkIn: string
  checkOut: string
  checkInLabel: string
  checkOutLabel: string
  nights: number
  calendarVisible: boolean
  availability: AppAvailabilityCheck | null
  checkingAvailability: boolean
  availabilityError: string
  bookingLoading: boolean
}

Page({
  data: {
    room: null,
    facilities: [],
    loading: true,
    errorMessage: '',
    imageIndex: 0,
    imageCount: 0,
    checkIn: '',
    checkOut: '',
    checkInLabel: '',
    checkOutLabel: '',
    nights: 0,
    calendarVisible: false,
    availability: null,
    checkingAvailability: false,
    availabilityError: '',
    bookingLoading: false,
  } as RoomDetailPageData,

  roomId: 0,
  quoteSequence: 0,

  async onLoad(options: Record<string, string | undefined>) {
    const roomId = Number(options.id)
    if (!Number.isInteger(roomId) || roomId <= 0) {
      this.setData({ loading: false, errorMessage: '房间编号无效' })
      return
    }
    this.roomId = roomId
    const dateWindow = getBookingDateWindow()
    const requestedIn = options.check_in || dateWindow.today
    const requestedOut = options.check_out || addDays(requestedIn, 1)
    const validRange = requestedIn >= dateWindow.today
      && requestedIn <= dateWindow.lastCheckInDate
      && requestedOut > requestedIn
      && requestedOut <= dateWindow.lastSelectableDate
    const checkIn = validRange ? requestedIn : dateWindow.today
    const checkOut = validRange ? requestedOut : addDays(checkIn, 1)
    this.setData({
      checkIn,
      checkOut,
      checkInLabel: formatMonthDay(checkIn),
      checkOutLabel: formatMonthDay(checkOut),
      nights: daysBetween(checkIn, checkOut),
    })
    await this.loadRoom()
    if (this.data.room) await this.checkSelectedStay()
  },

  async onPullDownRefresh() {
    await this.loadRoom()
    if (this.data.room) await this.checkSelectedStay()
    wx.stopPullDownRefresh()
  },

  async loadRoom() {
    this.setData({ loading: true, errorMessage: '' })
    try {
      const room = await getRoomDetail(this.roomId)
      this.setData({
        room,
        facilities: room.facilities.slice(0, 4).map((label) => ({ label, icon: getFacilityIcon(label) })),
        imageIndex: 0,
        imageCount: room.images.length,
      })
      wx.setNavigationBarTitle({ title: room.name })
    } catch (error) {
      this.setData({ errorMessage: error instanceof Error ? error.message : '房间详情读取失败' })
    } finally {
      this.setData({ loading: false })
    }
  },

  onGalleryChange(event: WechatMiniprogram.SwiperChange) {
    this.setData({ imageIndex: event.detail.current })
  },

  onGalleryStep(event: WechatMiniprogram.TouchEvent) {
    const room = this.data.room
    if (!room || room.images.length <= 1) return
    const delta = Number(event.currentTarget.dataset.delta)
    const imageIndex = (this.data.imageIndex + delta + room.images.length) % room.images.length
    this.setData({ imageIndex })
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
      nights: daysBetween(checkIn, checkOut),
      availability: null,
      availabilityError: '',
    })
    await this.checkSelectedStay()
  },

  async checkSelectedStay() {
    const quoteSequence = ++this.quoteSequence
    this.setData({ checkingAvailability: true, availabilityError: '', availability: null })
    try {
      const availability = await checkRoomAvailability(this.roomId, this.data.checkIn, this.data.checkOut)
      // 连续改期时，底栏只显示最新住宿日期的报价。
      if (quoteSequence === this.quoteSequence) this.setData({ availability })
    } catch (error) {
      if (quoteSequence !== this.quoteSequence) return
      this.setData({
        availabilityError: error instanceof Error ? error.message : '日期复核失败',
      })
    } finally {
      if (quoteSequence === this.quoteSequence) this.setData({ checkingAvailability: false })
    }
  },

  async onBookTap() {
    if (!this.data.room || this.data.bookingLoading || this.data.checkingAvailability) return
    const quoteSequence = this.quoteSequence
    this.setData({ bookingLoading: true, availabilityError: '' })
    try {
      const availability = await checkRoomAvailability(this.roomId, this.data.checkIn, this.data.checkOut)
      if (quoteSequence !== this.quoteSequence) return
      this.setData({ availability })
      if (!availability.available) {
        wx.showToast({ title: '所选日期该房型暂不可订', icon: 'none' })
        this.setData({ calendarVisible: true })
        return
      }
      const query = '?room_id=' + this.roomId
        + '&check_in=' + encodeURIComponent(this.data.checkIn)
        + '&check_out=' + encodeURIComponent(this.data.checkOut)
      wx.navigateTo({ url: '/pages/booking/index' + query })
    } catch (error) {
      if (quoteSequence !== this.quoteSequence) return
      this.setData({ availability: null, availabilityError: error instanceof Error ? error.message : '日期复核失败' })
      wx.showToast({ title: '暂时无法确认房态，请稍后重试', icon: 'none' })
    } finally {
      this.setData({ bookingLoading: false })
    }
  },

  noop() {},

  onBackTap() {
    wx.navigateBack({ delta: 1 })
  },
})

function formatMonthDay(date: string): string {
  const [, month, day] = date.split('-')
  return Number(month) + '月' + Number(day) + '日'
}

function getFacilityIcon(label: string): string {
  const normalized = label.toLowerCase()
  if (normalized.includes('wifi') || normalized.includes('wi-fi')) return 'wifi'
  if (label.includes('卫浴') || label.includes('浴室') || label.includes('卫生间')) return 'bath'
  if (label.includes('电视') || normalized.includes('tv')) return 'tv'
  if (label.includes('空调') || normalized.includes('air')) return 'wind'
  return 'info'
}
