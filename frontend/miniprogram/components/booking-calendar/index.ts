/** 小程序共享的入住与退房日期抽屉。 */
import { getRoomAvailability, getStoreCalendarAvailability, listRooms } from '../../services/rooms'
import type { CalendarAvailability } from '../../types/room'
import { addDays, daysBetween, getBookingDateWindow } from '../../utils/stay-date'

interface CalendarDay {
  key: string
  date: string
  label: string
  empty: boolean
  unavailable: boolean
  disabled: boolean
  selected: boolean
  inRange: boolean
  isToday: boolean
  isCheckIn: boolean
  isCheckOut: boolean
  priceLabel?: string
}

interface CalendarMonth {
  key: string
  month: string
  title: string
  days: CalendarDay[]
  availability?: CalendarAvailability
  loading: boolean
  error: string
}

interface CalendarPageData {
  today: string
  maxDate: string
  lastCheckInDate: string
  months: CalendarMonth[]
  tempCheckIn: string
  tempCheckOut: string
  selectingStep: 'checkin' | 'checkout'
  nights: number
  checkInPreview: string
  checkOutPreview: string
  loadVersion: number
  weekdays: string[]
}

Component({
  properties: {
    visible: {
      type: Boolean,
      value: false,
      observer(this: WechatMiniprogram.Component.TrivialInstance, visible: boolean) {
        if (visible) {
          const component = this as unknown as { openCalendar: () => void }
          component.openCalendar()
        }
      },
    },
    checkIn: { type: String, value: '' },
    checkOut: { type: String, value: '' },
    roomId: { type: Number, value: 0 },
  },
  data: {
    today: '',
    maxDate: '',
    lastCheckInDate: '',
    months: [],
    tempCheckIn: '',
    tempCheckOut: '',
    selectingStep: 'checkin',
    nights: 0,
    checkInPreview: '',
    checkOutPreview: '',
    loadVersion: 0,
    weekdays: ['日', '一', '二', '三', '四', '五', '六'],
  } as CalendarPageData,
  methods: {
    openCalendar() {
      const { today, lastSelectableDate, lastCheckInDate } = getBookingDateWindow()
      const requestedCheckIn = this.properties.checkIn
      const requestedCheckOut = this.properties.checkOut
      const hasValidSelection = requestedCheckIn >= today
        && requestedCheckIn <= lastCheckInDate
        && requestedCheckOut > requestedCheckIn
        && requestedCheckOut <= lastSelectableDate
      const checkIn = hasValidSelection ? requestedCheckIn : today
      const checkOut = hasValidSelection ? requestedCheckOut : addDays(checkIn, 1)
      const monthNames = getMonthsInRange(today, lastSelectableDate)
      const initialMonths = monthNames.map((month) => ({
        key: month,
        month,
        title: formatMonth(month),
        days: [],
        loading: true,
        error: '',
      }))

      const loadVersion = this.data.loadVersion + 1
      this.setData({
        loadVersion,
        today,
        maxDate: lastSelectableDate,
        lastCheckInDate,
        tempCheckIn: checkIn,
        tempCheckOut: checkOut,
        selectingStep: 'checkin',
        months: initialMonths,
        nights: daysBetween(checkIn, checkOut),
        checkInPreview: formatDayLabel(checkIn),
        checkOutPreview: formatDayLabel(checkOut),
      })
      this.rebuildDays(initialMonths, checkIn, checkOut, 'checkin')

      const roomId = this.properties.roomId
      // 每次打开都刷新价格和房态，全店模式只获取一次上架房间列表。
      const roomIds = roomId > 0 ? Promise.resolve([]) : listRooms().then((rooms) => rooms.map((room) => room.id))
      void Promise.all(initialMonths.map(async ({ month }) => {
        try {
          const availability = roomId > 0
            ? await getRoomAvailability(roomId, month)
            : await getStoreCalendarAvailability(month, await roomIds)
          if (this.data.loadVersion !== loadVersion) return
          const months = this.data.months.map((item) => item.month === month
            ? { ...item, loading: false, availability }
            : item)
          this.rebuildDays(months, this.data.tempCheckIn, this.data.tempCheckOut, this.data.selectingStep)
        } catch (error) {
          if (this.data.loadVersion !== loadVersion) return
          this.updateMonthError(month, error instanceof Error ? error.message : '房态日历读取失败')
        }
      }))
    },

    updateMonthError(month: string, message: string) {
      const months = this.data.months.map((item) => item.month === month
        ? {
          ...item,
          loading: false,
          error: message,
          days: item.days.map((day) => day.empty ? day : { ...day, disabled: true }),
        }
        : item)
      this.setData({ months })
    },

    onDayTap(event: WechatMiniprogram.TouchEvent) {
      const date = String(event.currentTarget.dataset.date)
      const month = String(event.currentTarget.dataset.month)
      const calendarMonth = this.data.months.find((item) => item.month === month)
      const day = calendarMonth?.days.find((item) => item.date === date)
      if (!day || day.empty || day.disabled || calendarMonth?.loading || calendarMonth?.error) return

      let checkIn = this.data.tempCheckIn
      let checkOut = this.data.tempCheckOut
      let selectingStep = this.data.selectingStep
      if (selectingStep === 'checkin' || date <= checkIn) {
        if (date > this.data.lastCheckInDate || day.unavailable) return
        checkIn = date
        checkOut = ''
        selectingStep = 'checkout'
      } else {
        // 退房当晚不占房，可选已满日期作退房；区间内的已满夜晚不可跨过。
        if (!this.verifyStayNights(checkIn, date)) return
        checkOut = date
        selectingStep = 'checkin'
      }
      this.updateSelection(checkIn, checkOut, selectingStep)
    },

    updateSelection(
      checkIn: string,
      checkOut: string,
      selectingStep: 'checkin' | 'checkout',
    ) {
      this.setData({
        tempCheckIn: checkIn,
        tempCheckOut: checkOut,
        selectingStep,
        nights: checkOut ? daysBetween(checkIn, checkOut) : 0,
        checkInPreview: formatDayLabel(checkIn),
        checkOutPreview: checkOut ? formatDayLabel(checkOut) : '请选择退房日期',
      })
      this.rebuildDays(this.data.months, checkIn, checkOut, selectingStep)
    },

    rebuildDays(
      months: CalendarMonth[],
      checkIn: string,
      checkOut: string,
      selectingStep: 'checkin' | 'checkout',
    ) {
      const nextMonths = months.map((month) => ({
        ...month,
        days: buildCalendarDays(
          month.month,
          this.data.today,
          this.data.maxDate,
          this.data.lastCheckInDate,
          month.availability,
          this.properties.roomId <= 0,
          month.loading || !!month.error,
          checkIn,
          checkOut,
          selectingStep,
        ),
      }))
      this.setData({ months: nextMonths })
    },

    onCloseTap() {
      this.triggerEvent('close')
    },

    onConfirmTap() {
      const { tempCheckIn, tempCheckOut } = this.data
      const { today, lastSelectableDate, lastCheckInDate } = getBookingDateWindow()
      if (!tempCheckOut || tempCheckIn < today || tempCheckIn > lastCheckInDate
        || tempCheckOut <= tempCheckIn || tempCheckOut > lastSelectableDate) return
      if (!this.verifyStayNights(tempCheckIn, tempCheckOut)) return
      this.triggerEvent('confirm', { checkIn: tempCheckIn, checkOut: tempCheckOut })
    },

    /** 房态加载完成后再确认，跨月时只核对实际占用的住宿夜晚。 */
    verifyStayNights(checkIn: string, checkOut: string): boolean {
      const firstMonth = checkIn.slice(0, 7)
      const lastMonth = addDays(checkOut, -1).slice(0, 7)
      const stayMonths = this.data.months.filter(({ month }) => month >= firstMonth && month <= lastMonth)
      if (stayMonths.some(({ error }) => !!error)) {
        wx.showToast({ title: '房态读取失败，请关闭日历后重新打开', icon: 'none' })
        return false
      }
      if (stayMonths.some(({ loading }) => loading)) {
        wx.showToast({ title: '正在读取所选日期的房态', icon: 'none' })
        return false
      }
      if (stayMonths.some(({ days }) => days.some((day) =>
        day.unavailable && day.date >= checkIn && day.date < checkOut,
      ))) {
        wx.showToast({ title: '所选区间含已满日期', icon: 'none' })
        return false
      }
      return true
    },

    noop() {},
  },
})

function buildCalendarDays(
  month: string,
  today: string,
  maxDate: string,
  lastCheckInDate: string,
  availability: CalendarAvailability | undefined,
  showStartingPrice: boolean,
  loadingOrError: boolean,
  checkIn: string,
  checkOut: string,
  selectingStep: 'checkin' | 'checkout',
): CalendarDay[] {
  const [year, monthNumber] = month.split('-').map(Number)
  const daysInMonth = new Date(Date.UTC(year, monthNumber, 0)).getUTCDate()
  const firstWeekday = new Date(Date.UTC(year, monthNumber - 1, 1)).getUTCDay()
  const unavailable = new Set(availability?.unavailable_dates)
  const days: CalendarDay[] = Array.from({ length: firstWeekday }, (_, index) => ({
    key: 'empty-' + month + '-' + index,
    date: '',
    label: '',
    empty: true,
    unavailable: false,
    disabled: true,
    selected: false,
    inRange: false,
    isToday: false,
    isCheckIn: false,
    isCheckOut: false,
  }))

  for (let number = 1; number <= daysInMonth; number += 1) {
    const date = month + '-' + String(number).padStart(2, '0')
    const withinWindow = date >= today && date <= maxDate
    const isUnavailable = withinWindow && (availability?.noRooms === true || unavailable.has(date))
    const isCheckIn = date === checkIn && !isUnavailable
    const isCheckOut = date === checkOut
    const selected = isCheckIn || isCheckOut
    const inRange = !!checkOut && date > checkIn && date < checkOut
    const price = withinWindow && date <= lastCheckInDate && !isUnavailable && !selected && !inRange
      ? availability?.nightly_prices?.[date] : undefined
    // 选择退房时保留入住日的选中样式；点击入住日会重新开始选择入住日期。
    const disabled = loadingOrError || !withinWindow || (selectingStep === 'checkout'
      ? date < checkIn
      : (date > lastCheckInDate || isUnavailable))
    days.push({
      key: date,
      date,
      label: number === Number(today.slice(-2)) && month === today.slice(0, 7) ? '今' : String(number),
      empty: false,
      unavailable: isUnavailable,
      disabled,
      selected,
      inRange,
      isToday: date === today,
      isCheckIn,
      isCheckOut,
      priceLabel: price === undefined ? '' : formatNightlyPrice(price, showStartingPrice),
    })
  }

  // 补齐最后一周，保证只有少数日期的末行也占满七列并与星期标题对齐。
  while (days.length % 7 !== 0) {
    days.push({
      key: 'empty-' + month + '-tail-' + days.length,
      date: '',
      label: '',
      empty: true,
      unavailable: false,
      disabled: true,
      selected: false,
      inRange: false,
      isToday: false,
      isCheckIn: false,
      isCheckOut: false,
    })
  }

  return days
}

function formatNightlyPrice(price: string, showStartingPrice: boolean): string {
  const amount = price.replace(/\.00$/, '').replace(/(\.\d)0$/, '$1')
  return '¥' + amount + (showStartingPrice ? '起' : '')
}

function getMonthsInRange(start: string, end: string): string[] {
  const months: string[] = []
  let current = start.slice(0, 7)
  const last = end.slice(0, 7)
  while (current <= last) {
    months.push(current)
    const [year, month] = current.split('-').map(Number)
    const next = new Date(Date.UTC(year, month, 1))
    current = next.getUTCFullYear() + '-' + String(next.getUTCMonth() + 1).padStart(2, '0')
  }
  return months
}

function formatMonth(month: string): string {
  const [year, monthNumber] = month.split('-')
  return year + '年 ' + Number(monthNumber) + '月'
}

function formatDayLabel(date: string): string {
  const [year, month, day] = date.split('-').map(Number)
  const weekday = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][new Date(Date.UTC(year, month - 1, day)).getUTCDay()]
  return month + '月' + day + '日 ' + weekday
}
