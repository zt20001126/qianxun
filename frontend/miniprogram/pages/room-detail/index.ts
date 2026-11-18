/** R02/R03/R04 小程序房间详情和 60 天房态复核。 */
import { checkRoomAvailability, getRoomAvailability, getRoomDetail } from '../../services/rooms'
import type { AppAvailabilityCheck, AppRoomDetail } from '../../types/room'
import { addDays, getBookingDateWindow } from '../../utils/stay-date'

interface CalendarDay {
  key: string
  date: string
  label: string
  empty: boolean
  unavailable: boolean
  disabled: boolean
  selected: boolean
  inRange: boolean
}

interface RoomDetailPageData {
  room: AppRoomDetail | null
  loading: boolean
  errorMessage: string
  today: string
  maxDate: string
  currentMonth: string
  monthLabel: string
  canGoPrevious: boolean
  canGoNext: boolean
  weekdays: string[]
  calendarDays: CalendarDay[]
  calendarLoading: boolean
  calendarError: string
  checkIn: string
  checkOut: string
  checkInLabel: string
  checkOutLabel: string
  checkResult: AppAvailabilityCheck | null
  checkLoading: boolean
}

Page({
  data: {
    room: null,
    loading: true,
    errorMessage: '',
    today: '',
    maxDate: '',
    currentMonth: '',
    monthLabel: '',
    canGoPrevious: false,
    canGoNext: false,
    weekdays: ['日', '一', '二', '三', '四', '五', '六'],
    calendarDays: [],
    calendarLoading: true,
    calendarError: '',
    checkIn: '',
    checkOut: '',
    checkInLabel: '请选择入住日期',
    checkOutLabel: '请选择退房日期',
    checkResult: null,
    checkLoading: false,
  } as RoomDetailPageData,

  async onLoad(options: Record<string, string | undefined>) {
    const roomId = Number(options.id)
    if (!Number.isInteger(roomId) || roomId <= 0) {
      this.setData({ loading: false, errorMessage: '房间编号无效' })
      return
    }
    const { today, lastSelectableDate } = getBookingDateWindow()
    const currentMonth = today.slice(0, 7)
    this.setData({
      today,
      maxDate: lastSelectableDate,
      currentMonth,
      monthLabel: formatMonth(currentMonth),
      canGoPrevious: false,
      canGoNext: currentMonth < lastSelectableDate.slice(0, 7),
    })
    await Promise.all([this.loadRoom(roomId), this.loadCalendar(roomId, currentMonth)])
  },

  async onPullDownRefresh() {
    const roomId = this.data.room?.id
    if (roomId) {
      await Promise.all([this.loadRoom(roomId), this.loadCalendar(roomId, this.data.currentMonth)])
    }
    wx.stopPullDownRefresh()
  },

  async loadRoom(roomId: number) {
    this.setData({ loading: true, errorMessage: '' })
    try {
      const room = await getRoomDetail(roomId)
      this.setData({ room })
      wx.setNavigationBarTitle({ title: room.name })
    } catch (error) {
      this.setData({ errorMessage: error instanceof Error ? error.message : '房间详情读取失败' })
    } finally {
      this.setData({ loading: false })
    }
  },

  async loadCalendar(roomId: number, month: string) {
    this.setData({ calendarLoading: true, calendarError: '' })
    try {
      const availability = await getRoomAvailability(roomId, month)
      this.setData({
        currentMonth: month,
        monthLabel: formatMonth(month),
        calendarDays: buildCalendarDays(
          month,
          this.data.today,
          this.data.maxDate,
          availability.unavailable_dates,
          this.data.checkIn,
          this.data.checkOut,
        ),
      })
    } catch (error) {
      this.setData({ calendarError: error instanceof Error ? error.message : '房态日历读取失败' })
    } finally {
      this.setData({ calendarLoading: false })
    }
  },

  async onMonthStep(event: WechatMiniprogram.TouchEvent) {
    if (this.data.checkLoading || this.data.calendarLoading) return
    const delta = Number(event.currentTarget.dataset.delta)
    const nextMonth = shiftMonth(this.data.currentMonth, delta)
    const firstMonth = this.data.today.slice(0, 7)
    const lastMonth = this.data.maxDate.slice(0, 7)
    if (nextMonth < firstMonth || nextMonth > lastMonth || !this.data.room) return
    this.setData({ canGoPrevious: nextMonth > firstMonth, canGoNext: nextMonth < lastMonth })
    await this.loadCalendar(this.data.room.id, nextMonth)
  },

  async onDayTap(event: WechatMiniprogram.TouchEvent) {
    if (this.data.checkLoading || this.data.calendarLoading) return
    const date = String(event.currentTarget.dataset.date)
    const day = this.data.calendarDays.find((item) => item.date === date)
    if (!day || day.empty || day.disabled) return

    if (!this.data.checkIn || this.data.checkOut) {
      this.setData({
        checkIn: date,
        checkOut: '',
        checkInLabel: date,
        checkOutLabel: '请选择退房日期',
        checkResult: null,
      })
      await this.refreshCalendar()
      return
    }

    if (date <= this.data.checkIn) {
      this.setData({
        checkIn: date,
        checkOut: '',
        checkInLabel: date,
        checkOutLabel: '请选择退房日期',
        checkResult: null,
      })
      await this.refreshCalendar()
      return
    }

    const checkIn = this.data.checkIn
    const checkOut = date
    this.setData({ checkOut, checkOutLabel: checkOut, checkLoading: true, checkResult: null, calendarError: '' })
    await this.refreshCalendar(checkIn, checkOut)
    try {
      const result = await checkRoomAvailability(this.data.room!.id, checkIn, checkOut)
      this.setData({ checkResult: result })
    } catch (error) {
      this.setData({ calendarError: error instanceof Error ? error.message : '日期复核失败' })
    } finally {
      this.setData({ checkLoading: false })
    }
  },

  async refreshCalendar(checkIn?: string, checkOut?: string) {
    if (!this.data.room) return
    const selectedCheckIn = checkIn ?? this.data.checkIn
    const selectedCheckOut = checkOut ?? this.data.checkOut
    try {
      const availability = await getRoomAvailability(this.data.room.id, this.data.currentMonth)
      this.setData({
        calendarDays: buildCalendarDays(
          this.data.currentMonth,
          this.data.today,
          this.data.maxDate,
          availability.unavailable_dates,
          selectedCheckIn,
          selectedCheckOut,
        ),
      })
    } catch (error) {
      this.setData({ calendarError: error instanceof Error ? error.message : '房态日历读取失败' })
    }
  },

  onSearchStayRooms() {
    if (!this.data.checkResult?.available || !this.data.checkIn || !this.data.checkOut) return
    wx.setStorageSync('qx_stay_selection', { check_in: this.data.checkIn, check_out: this.data.checkOut })
    wx.navigateBack({ delta: 1 })
  },

  onBackTap() {
    wx.navigateBack({ delta: 1 })
  },
})

function buildCalendarDays(
  month: string,
  today: string,
  maxDate: string,
  unavailableDates: string[],
  checkIn: string,
  checkOut: string,
): CalendarDay[] {
  const [year, monthNumber] = month.split('-').map(Number)
  const daysInMonth = new Date(Date.UTC(year, monthNumber, 0)).getUTCDate()
  const firstWeekday = new Date(Date.UTC(year, monthNumber - 1, 1)).getUTCDay()
  const unavailable = new Set(unavailableDates)
  const selectingCheckout = !!checkIn && !checkOut
  const days: CalendarDay[] = Array.from({ length: firstWeekday }, (_, index) => ({
    key: `empty-${index}`,
    date: '',
    label: '',
    empty: true,
    unavailable: false,
    disabled: true,
    selected: false,
    inRange: false,
  }))

  for (let number = 1; number <= daysInMonth; number += 1) {
    const date = `${month}-${String(number).padStart(2, '0')}`
    const isUnavailable = unavailable.has(date)
    const withinWindow = date >= today && date <= maxDate
    const canChooseAsCheckout = selectingCheckout && date > checkIn && withinWindow
    const canChooseAsCheckIn = withinWindow && date < maxDate
    const disabled = !withinWindow || (selectingCheckout ? !canChooseAsCheckout : (!canChooseAsCheckIn || isUnavailable))
    days.push({
      key: date,
      date,
      label: String(number),
      empty: false,
      unavailable: isUnavailable,
      disabled,
      selected: date === checkIn || date === checkOut,
      inRange: !!checkIn && !!checkOut && date >= checkIn && date <= checkOut,
    })
  }
  return days
}

function formatMonth(month: string): string {
  const [year, monthNumber] = month.split('-')
  return `${year} 年 ${Number(monthNumber)} 月`
}

function shiftMonth(month: string, amount: number): string {
  const [year, monthNumber] = month.split('-').map(Number)
  const shifted = new Date(Date.UTC(year, monthNumber - 1 + amount, 1))
  return `${shifted.getUTCFullYear()}-${String(shifted.getUTCMonth() + 1).padStart(2, '0')}`
}
