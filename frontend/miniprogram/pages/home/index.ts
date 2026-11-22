/** 小程序首页：展示水乡品牌视觉、行程选择和导览入口。 */
import { listRooms } from '../../services/rooms'
import type { AppRoomSummary } from '../../types/room'
import { STORE_ADDRESS, STORE_CONTACT_PHONE_LABEL } from '../../config'
import { addDays, daysBetween, getBookingDateWindow } from '../../utils/stay-date'

interface GuideContent {
  tag: string
  title: string
  body: string
  extra: string
}

interface HomePageData {
  today: string
  maxDate: string
  lastCheckInDate: string
  checkIn: string
  checkOut: string
  checkInLabel: string
  checkOutLabel: string
  checkInWeek: string
  checkOutWeek: string
  nights: number
  roomCount: number
  searchButtonLabel: string
  roomsLoading: boolean
  roomsError: string
  calendarVisible: boolean
  guideVisible: boolean
  guide: GuideContent
}

const GUIDE_CONTENT: Record<string, GuideContent> = {
  intro: {
    tag: '关于芊浔',
    title: '芊浔民宿',
    body: `芊浔民宿位于${STORE_ADDRESS}，靠近百间楼片区，共有六间客房。房型、床型和设施请查看房间详情。`,
    extra: `地址：${STORE_ADDRESS}`,
  },
  around: {
    tag: '古镇漫游',
    title: '百间楼周边',
    body: '附近可游览百间楼、张静江故居和东大街，沿河感受古镇水乡风貌。文保建筑开放与售票信息以景区公告为准。',
    extra: '南浔古镇东北片区',
  },
  traffic: {
    tag: '出行指南',
    title: '到店交通',
    body: `导航搜索“${STORE_ADDRESS}”。乘高铁到湖州南浔站后，请按实时地图查询公交或打车路线；古镇内停车与步行路线以现场指引为准。`,
    extra: `联系电话：${STORE_CONTACT_PHONE_LABEL}`,
  },
  rooms: {
    tag: '雅居一览',
    title: '查看客房与设施',
    body: '芊浔民宿设有六间客房。选择入住与退房日期后，可查询整段行程的可订房间；床型、面积与设施请查看对应房间详情。',
    extra: '点击“查询房间”查看当前日期可订房型',
  },
  nanxun: {
    tag: '南浔漫游',
    title: '在南浔，慢游古镇',
    body: `芊浔民宿位于${STORE_ADDRESS}。古镇景点开放时间与游览路线请以景区公告和地图实时信息为准。`,
    extra: `民宿地址：${STORE_ADDRESS}`,
  },
}

import { consumePendingInvitation } from '../../utils/invitation'

Page({
  data: {
    today: '',
    maxDate: '',
    lastCheckInDate: '',
    checkIn: '',
    checkOut: '',
    checkInLabel: '',
    checkOutLabel: '',
    checkInWeek: '',
    checkOutWeek: '',
    nights: 0,
    roomCount: 0,
    searchButtonLabel: '查询房间（0间可订）',
    roomsLoading: false,
    roomsError: '',
    calendarVisible: false,
    guideVisible: false,
    guide: { tag: '', title: '', body: '', extra: '' },
  } as HomePageData,

  onShow() {
    void consumePendingInvitation()
  },

  async onLoad() {
    const { today, lastSelectableDate, lastCheckInDate } = getBookingDateWindow()
    const checkOut = addDays(today, 1)
    this.setData({
      today,
      maxDate: lastSelectableDate,
      lastCheckInDate,
      checkIn: today,
      checkOut,
      checkInLabel: formatMonthDay(today),
      checkOutLabel: formatMonthDay(checkOut),
      checkInWeek: formatWeek(today),
      checkOutWeek: formatWeek(checkOut),
      nights: 1,
    })
    await this.loadAvailableRooms(today, checkOut)
  },

  onPullDownRefresh() {
    void this.loadAvailableRooms(this.data.checkIn, this.data.checkOut)
      .finally(() => wx.stopPullDownRefresh())
  },

  async loadAvailableRooms(checkIn: string, checkOut: string) {
    this.setData({ roomsLoading: true, roomsError: '' })
    try {
      const rooms: AppRoomSummary[] = await listRooms(checkIn, checkOut)
      this.setData({
        roomCount: rooms.length,
        searchButtonLabel: '查询房间（' + rooms.length + '间可订）',
      })
    } catch (error) {
      this.setData({
        roomCount: 0,
        searchButtonLabel: '查询房间（0间可订）',
        roomsError: error instanceof Error ? error.message : '房态读取失败',
      })
    } finally {
      this.setData({ roomsLoading: false })
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
    await this.loadAvailableRooms(checkIn, checkOut)
  },

  onSearchTap() {
    if (!this.data.checkIn || !this.data.checkOut || this.data.checkOut <= this.data.checkIn) {
      wx.showToast({ title: '请选择有效的入住和退房日期', icon: 'none' })
      return
    }
    const query = '?check_in=' + encodeURIComponent(this.data.checkIn)
      + '&check_out=' + encodeURIComponent(this.data.checkOut)
    wx.navigateTo({ url: '/pages/rooms/index' + query })
  },

  onGuideTap(event: WechatMiniprogram.TouchEvent) {
    const type = String(event.currentTarget.dataset.type)
    const guide = GUIDE_CONTENT[type]
    if (guide) this.setData({ guide, guideVisible: true })
  },

  onGuideClose() {
    this.setData({ guideVisible: false })
  },

  noop() {},

  onMyOrdersTap() {
    wx.navigateTo({ url: '/pages/orders/index' })
  },

  onLogoutTap() {
    wx.reLaunch({ url: '/pages/login/index' })
  },
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
