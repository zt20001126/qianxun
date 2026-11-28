/** 可订房间列表：按完整入住区间展示后端返回的房源。 */
import { getRoomDetail, listRooms } from '../../services/rooms'
import type { AppRoomSummary } from '../../types/room'
import { addDays, daysBetween, getBookingDateWindow } from '../../utils/stay-date'

interface ListedRoom extends AppRoomSummary {
  area: number | null
  areaLabel: string
  priceLabel: string
}

interface RoomsPageData {
  rooms: ListedRoom[]
  checkIn: string
  checkOut: string
  checkInLabel: string
  checkOutLabel: string
  checkInWeek: string
  checkOutWeek: string
  nights: number
  loading: boolean
  errorMessage: string
  calendarVisible: boolean
}

Page({
  data: {
    rooms: [],
    checkIn: '',
    checkOut: '',
    checkInLabel: '',
    checkOutLabel: '',
    checkInWeek: '',
    checkOutWeek: '',
    nights: 0,
    loading: true,
    errorMessage: '',
    calendarVisible: false,
  } as RoomsPageData,

  loadSequence: 0,

  async onLoad(options: Record<string, string | undefined>) {
    const window = getBookingDateWindow()
    const requestedIn = options.check_in || window.today
    const requestedOut = options.check_out || addDays(requestedIn, 1)
    const validRange = requestedIn >= window.today
      && requestedIn <= window.lastCheckInDate
      && requestedOut > requestedIn
      && requestedOut <= window.lastSelectableDate
    const checkIn = validRange ? requestedIn : window.today
    const checkOut = validRange ? requestedOut : addDays(checkIn, 1)
    this.setData({
      checkIn,
      checkOut,
      checkInLabel: formatMonthDay(checkIn),
      checkOutLabel: formatMonthDay(checkOut),
      checkInWeek: formatWeek(checkIn),
      checkOutWeek: formatWeek(checkOut),
      nights: daysBetween(checkIn, checkOut),
    })
    await this.loadRooms()
  },

  async loadRooms() {
    const loadSequence = ++this.loadSequence
    const { checkIn, checkOut, nights } = this.data
    this.setData({ loading: true, errorMessage: '' })
    try {
      const summaries = await listRooms(checkIn, checkOut)
      const rooms = await Promise.all(summaries.map(async (room) => {
        const priceLabel = room.room_amount != null && nights > 1
          ? formatNightlyAverage(room.room_amount, nights)
          : formatPrice(room.room_amount ?? room.price)
        try {
          const detail = await getRoomDetail(room.id)
          return {
            ...room,
            area: detail.area,
            areaLabel: detail.area ? detail.area + '㎡' : '—',
            priceLabel,
          }
        } catch {
          return { ...room, area: null, areaLabel: '—', priceLabel }
        }
      }))
      // 切换日期后只接受最新查询，避免把旧区间房费显示在新日期上。
      if (loadSequence === this.loadSequence) this.setData({ rooms })
    } catch (error) {
      if (loadSequence !== this.loadSequence) return
      this.setData({
        rooms: [],
        errorMessage: error instanceof Error ? error.message : '可订房间读取失败',
      })
    } finally {
      if (loadSequence === this.loadSequence) this.setData({ loading: false })
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
    })
    await this.loadRooms()
  },

  onRoomTap(event: WechatMiniprogram.TouchEvent) {
    const roomId = Number(event.currentTarget.dataset.roomId)
    if (!Number.isInteger(roomId) || roomId <= 0) return
    const query = '?id=' + roomId
      + '&check_in=' + encodeURIComponent(this.data.checkIn)
      + '&check_out=' + encodeURIComponent(this.data.checkOut)
    wx.navigateTo({ url: '/pages/room-detail/index' + query })
  },

  noop() {},
})

/** 从后端房费合计按分四舍五入计算展示均价。 */
function formatNightlyAverage(amount: string, nights: number): string {
  const [yuan, fraction = ''] = amount.split('.')
  const cents = Number(yuan + fraction.padEnd(2, '0'))
  const averageCents = Math.floor((cents + Math.floor(nights / 2)) / nights)
  const digits = String(averageCents).padStart(3, '0')
  return formatPrice(`${digits.slice(0, -2)}.${digits.slice(-2)}`)
}

function formatPrice(price: string): string {
  return price.replace(/\.00$/, '')
}

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

