/** 小程序首页：展示水乡品牌视觉、行程选择和导览入口。 */
import { listRooms } from '../../services/rooms'
import type { AppRoomSummary } from '../../types/room'
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
    title: '芊浔民宿 · 枕水而居',
    body: '坐落于浙江省湖州市南浔区南浔古镇88号，全店精设 6 间水乡雅居（一楼 101/102/103、二楼 201/202/203），保留徽派马头墙与雕花木窗，推窗即见摇橹船影。',
    extra: '地址：浙江省湖州市南浔区南浔古镇88号',
  },
  around: {
    tag: '水乡漫游',
    title: '周边必打卡景点',
    body: '• 百间楼古民居群（步行 1 分钟，晨雾与灯笼夜色绝美）\n• 小莲庄 & 嘉业堂藏书楼（步行 6 分钟）\n• 张石铭旧宅（步行 8 分钟，中西合璧建筑瑰宝）',
    extra: '每日 19:30 庭院免费奉上南浔三道茶',
  },
  traffic: {
    tag: '出行指南',
    title: '如何抵达芊浔民宿',
    body: '• 高铁出行：乘至「湖州南浔站」，打车约 15 分钟即达古镇东门停车场。\n• 自驾导航：搜索「南浔古镇东门地面停车场」，管家将提前至入口迎候接行李。',
    extra: '24小时管家热线：0572-1234 5678',
  },
  rooms: {
    tag: '雅居一览',
    title: '寻一间临水雅室',
    body: '芊浔民宿设有六间水乡客房。选择入住与退房日期后，系统会自动筛选整段行程可订的房间；每间房的床型、面积与设施可在房间详情中查看。',
    extra: '点击“查询房间”查看当前日期可订房型',
  },
  nanxun: {
    tag: '文化底蕴',
    title: '百间楼夜色 · 江南水乡韵',
    body: '南浔古镇素有“文化之邦”与“诗书之乡”美誉，相比喧闹商业古镇，这里仍保留着原住民临河浣洗、茶馆听评弹的烟火气。入住芊浔，私享整片静谧水乡。',
    extra: '营业时间：全年无休 · 14:00 入住 / 次日 12:00 退房',
  },
}

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
    wx.switchTab({ url: '/pages/orders/index' })
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
