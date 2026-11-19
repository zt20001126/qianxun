/** 小程序共享的未来 60 天入住与退房日期抽屉。 */
import { getFullyUnavailableDates, getRoomAvailability } from '../../services/rooms'
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
}

interface CalendarMonth {
  key: string
  month: string
  title: string
  days: CalendarDay[]
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
  availabilityCache: Record<string, string[]>
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
    availabilityCache: {},
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

      this.setData({
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
      this.rebuildDays(initialMonths, checkIn, checkOut, 'checkin', {})

      const roomId = this.properties.roomId
      void Promise.all(initialMonths.map(async (month) => {
        const cachedDates = this.data.availabilityCache[month.month]
        if (cachedDates) {
          this.updateMonthAvailability(month.month, cachedDates)
          return
        }
        try {
          const unavailableDates = roomId > 0
            ? (await getRoomAvailability(roomId, month.month)).unavailable_dates
            : await getFullyUnavailableDates(month.month)
          this.updateMonthAvailability(month.month, unavailableDates)
        } catch (error) {
          this.updateMonthError(month.month, error instanceof Error ? error.message : '房态日历读取失败')
        }
      }))
    },

    updateMonthAvailability(month: string, unavailableDates: string[]) {
      this.setData({
        availabilityCache: {
          ...this.data.availabilityCache,
          [month]: unavailableDates,
        },
      })
      const currentMonths = this.data.months
      const nextMonths = currentMonths.map((item) => item.month === month
        ? { ...item, loading: false, error: '', days: buildCalendarDays(
          item.month,
          this.data.today,
          this.data.maxDate,
          this.data.lastCheckInDate,
          unavailableDates,
          this.data.tempCheckIn,
          this.data.tempCheckOut,
          this.data.selectingStep,
        ) }
        : item)
      this.setData({ months: nextMonths })
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
      const day = this.data.months.find((item) => item.month === month)?.days.find((item) => item.date === date)
      if (!day || day.empty || day.disabled) return

      let checkIn = this.data.tempCheckIn
      let checkOut = this.data.tempCheckOut
      let selectingStep = this.data.selectingStep
      if (selectingStep === 'checkin' || date <= checkIn) {
        if (date > this.data.lastCheckInDate || day.unavailable) return
        checkIn = date
        checkOut = ''
        selectingStep = 'checkout'
      } else {
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
      const unavailableByMonth: Record<string, string[]> = {}
      this.data.months.forEach((month) => {
        unavailableByMonth[month.month] = month.days.filter((day) => day.unavailable).map((day) => day.date)
      })
      this.rebuildDays(this.data.months, checkIn, checkOut, selectingStep, unavailableByMonth)
    },

    rebuildDays(
      months: CalendarMonth[],
      checkIn: string,
      checkOut: string,
      selectingStep: 'checkin' | 'checkout',
      unavailableByMonth: Record<string, string[]>,
    ) {
      const nextMonths = months.map((month) => ({
        ...month,
        days: buildCalendarDays(
          month.month,
          this.data.today,
          this.data.maxDate,
          this.data.lastCheckInDate,
          unavailableByMonth[month.month] || [],
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
      const { tempCheckIn, tempCheckOut, today, maxDate, lastCheckInDate } = this.data
      if (!tempCheckOut || tempCheckIn < today || tempCheckIn > lastCheckInDate
        || tempCheckOut <= tempCheckIn || tempCheckOut > maxDate) return
      this.triggerEvent('confirm', { checkIn: tempCheckIn, checkOut: tempCheckOut })
    },

    noop() {},
  },
})

function buildCalendarDays(
  month: string,
  today: string,
  maxDate: string,
  lastCheckInDate: string,
  unavailableDates: string[],
  checkIn: string,
  checkOut: string,
  selectingStep: 'checkin' | 'checkout',
): CalendarDay[] {
  const [year, monthNumber] = month.split('-').map(Number)
  const daysInMonth = new Date(Date.UTC(year, monthNumber, 0)).getUTCDate()
  const firstWeekday = new Date(Date.UTC(year, monthNumber - 1, 1)).getUTCDay()
  const unavailable = new Set(unavailableDates)
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
    const isUnavailable = unavailable.has(date)
    const withinWindow = date >= today && date <= maxDate
    // 选择退房时保留入住日的选中样式；点击入住日会重新开始选择入住日期。
    const disabled = !withinWindow || (selectingStep === 'checkout'
      ? date < checkIn
      : (date > lastCheckInDate || isUnavailable))
    days.push({
      key: date,
      date,
      label: number === Number(today.slice(-2)) && month === today.slice(0, 7) ? '今' : String(number),
      empty: false,
      unavailable: isUnavailable,
      disabled,
      selected: date === checkIn || (!!checkOut && date === checkOut),
      inRange: !!checkIn && !!checkOut && date > checkIn && date < checkOut,
      isToday: date === today,
      isCheckIn: date === checkIn,
      isCheckOut: date === checkOut,
    })
  }
  return days
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
