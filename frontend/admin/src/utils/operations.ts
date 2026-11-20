import type { RoomBlock } from '../api/room-blocks'

/** 经营总览与房态日历共用的日期、金额分摊与状态展示工具。 */

const WEEKDAYS = ['日', '一', '二', '三', '四', '五', '六'] as const

export const ROOM_STATUS_LABELS: Record<string, string> = {
  AVAILABLE: '空房',
  BLOCKED: '不可预订',
  PENDING_CONFIRM: '已预订',
  PENDING_CHECKIN: '待入住',
  CHECKED_IN: '已入住',
  COMPLETED: '已退房',
  OCCUPIED: '占用',
}

/** 获取当前北京时间的 YYYY-MM-DD 日期字符串。 */
export function shanghaiDate(value = new Date()): string {
  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone: 'Asia/Shanghai',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(value)
  const part = (type: string) => parts.find((item) => item.type === type)?.value || ''
  return `${part('year')}-${part('month')}-${part('day')}`
}

/** 格式化为中文日期，如 2026年11月20日。 */
export function formatChineseDate(ymd: string): string {
  const [year, month, day] = ymd.split('-')
  if (!year || !month || !day) return ymd
  return `${year}年${month}月${day}日`
}

/** 对 YYYY-MM-DD 日期进行天数偏移计算。 */
export function addDays(ymd: string, delta: number): string {
  const [year, month, day] = ymd.split('-').map(Number)
  const next = new Date(Date.UTC(year, month - 1, day + delta))
  return `${next.getUTCFullYear()}-${String(next.getUTCMonth() + 1).padStart(2, '0')}-${String(next.getUTCDate()).padStart(2, '0')}`
}

/** 计算两个 YYYY-MM-DD 日期的间隔天数（endDate - startDate）。 */
export function dayDifference(startDate: string, endDate: string): number {
  const [sy, sm, sd] = startDate.split('-').map(Number)
  const [ey, em, ed] = endDate.split('-').map(Number)
  const start = Date.UTC(sy, sm - 1, sd)
  const end = Date.UTC(ey, em - 1, ed)
  return Math.round((end - start) / 86_400_000)
}

/** 对 YYYY-MM 月份字符串进行月份偏移计算。 */
export function shiftMonthValue(month: string, delta: number): string {
  const [year, m] = month.split('-').map(Number)
  const next = new Date(Date.UTC(year, m - 1 + delta, 1))
  return `${next.getUTCFullYear()}-${String(next.getUTCMonth() + 1).padStart(2, '0')}`
}

/** 返回指定 YYYY-MM 月份的总天数。 */
export function monthDayCount(month: string): number {
  const [year, m] = month.split('-').map(Number)
  if (!year || !m) return 0
  return new Date(Date.UTC(year, m, 0)).getUTCDate()
}

/** 生成指定 YYYY-MM 月份的全部日期列表。 */
export function monthDateList(month: string): string[] {
  const count = monthDayCount(month)
  return Array.from({ length: count }, (_, idx) => `${month}-${String(idx + 1).padStart(2, '0')}`)
}

/** 返回日期对应的中文星期简称（日～六）。 */
export function weekdayShort(ymd: string): string {
  const [year, month, day] = ymd.split('-').map(Number)
  const dayOfWeek = new Date(Date.UTC(year, month - 1, day)).getUTCDay()
  return WEEKDAYS[dayOfWeek] ?? ''
}

/** 判断日期是否为周六或周日。 */
export function isWeekendDate(ymd: string): boolean {
  const [year, month, day] = ymd.split('-').map(Number)
  const dayOfWeek = new Date(Date.UTC(year, month - 1, day)).getUTCDay()
  return dayOfWeek === 0 || dayOfWeek === 6
}

/** 十进制金额字符串转为分，避免浮点误差。 */
export function amountToCents(value: string | number | null | undefined): number {
  if (value == null) return 0
  const amount = String(value).trim()
  if (!amount) return 0
  const negative = amount.startsWith('-')
  const [whole = '0', fraction = ''] = amount.replace(/^-/, '').split('.')
  const cents = Number(whole) * 100 + Number((fraction + '00').slice(0, 2))
  if (Number.isNaN(cents)) return 0
  return negative ? -cents : cents
}

/** 分转换为保留两位小数的十进制金额字符串。 */
export function centsToAmount(cents: number): string {
  const sign = cents < 0 ? '-' : ''
  const absolute = Math.abs(Math.round(cents))
  return `${sign}${Math.floor(absolute / 100)}.${String(absolute % 100).padStart(2, '0')}`
}

/** 两个十进制金额字符串相加。 */
export function addAmounts(first: string, second: string): string {
  return centsToAmount(amountToCents(first) + amountToCents(second))
}

/** 按住宿晚数平均分摊金额到第 nightIndex 晚，分币尾差归最后一晚。 */
export function allocateNight(amount: string, nightIndex: number, nights: number): number {
  const cents = amountToCents(amount)
  const baseCents = Math.floor(cents / nights)
  const remainder = cents - baseCents * nights
  return baseCents + (nightIndex === nights - 1 ? remainder : 0)
}

/** 校验非负金额字符串（最多两位小数）。 */
export function isMoneyString(value: string): boolean {
  return /^\d+(\.\d{1,2})?$/.test(value.trim())
}

/** 统计指定月份内所有管理员封房覆盖的总间夜数（去重按房+日计）。 */
export function countBlockedNightsInMonth(blocks: RoomBlock[], month: string): number {
  const monthStart = `${month}-01`
  const nextMonthStart = `${shiftMonthValue(month, 1)}-01`
  const uniqueNights = new Set<string>()
  for (const block of blocks) {
    const start = block.start_date > monthStart ? block.start_date : monthStart
    const end = block.end_date < nextMonthStart ? block.end_date : nextMonthStart
    const nights = dayDifference(start, end)
    for (let i = 0; i < nights; i++) {
      uniqueNights.add(`${block.room_id}:${addDays(start, i)}`)
    }
  }
  return uniqueNights.size
}

/** 房态编码转换为中文展示标签。 */
export function roomStatusLabel(status: string): string {
  return ROOM_STATUS_LABELS[status] || status
}
