/** 小程序共用的北京时间和住宿日期窗口计算。 */
const SHANGHAI_OFFSET_MILLISECONDS = 8 * 60 * 60 * 1000
const BOOKING_WINDOW_DAYS = 90
const MILLISECONDS_PER_DAY = 24 * 60 * 60 * 1000

export interface BookingDateWindow {
  today: string
  lastSelectableDate: string
  lastCheckInDate: string
}

/** 以北京时间返回今天及入住/退房可选上限。 */
export function getBookingDateWindow(): BookingDateWindow {
  const today = todayInShanghai()
  return {
    today,
    lastSelectableDate: addDays(today, BOOKING_WINDOW_DAYS - 1),
    // 退房日也必须落在窗口内，因此最后一个可入住日需提前一晚。
    lastCheckInDate: addDays(today, BOOKING_WINDOW_DAYS - 2),
  }
}

/** 按固定住宿日历加减自然日，避免设备时区和夏令时影响。 */
export function addDays(date: string, days: number): string {
  const [year, month, day] = date.split('-').map(Number)
  return new Date(Date.UTC(year, month - 1, day + days)).toISOString().slice(0, 10)
}

/** 计算两个 ISO 日期之间的住宿晚数。 */
export function daysBetween(start: string, end: string): number {
  if (!start || !end || end <= start) return 0
  const [startYear, startMonth, startDay] = start.split('-').map(Number)
  const [endYear, endMonth, endDay] = end.split('-').map(Number)
  const startTime = Date.UTC(startYear, startMonth - 1, startDay)
  const endTime = Date.UTC(endYear, endMonth - 1, endDay)
  return Math.round((endTime - startTime) / MILLISECONDS_PER_DAY)
}

function todayInShanghai(): string {
  return new Date(Date.now() + SHANGHAI_OFFSET_MILLISECONDS).toISOString().slice(0, 10)
}
