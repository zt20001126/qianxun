/** 小程序首页：展示会员简要资料和可浏览客房。 */
import { DEFAULT_AVATAR_PATH } from '../../config'
import { getCurrentMember } from '../../services/auth'
import { listRooms } from '../../services/rooms'
import type { MemberProfile } from '../../types/auth'
import type { AppRoomSummary } from '../../types/room'
import { clearSession, saveCurrentMember } from '../../utils/session'
import { addDays, daysBetween, getBookingDateWindow } from '../../utils/stay-date'

interface HomePageData {
  member: MemberProfile | null
  rooms: AppRoomSummary[]
  avatarSource: string
  memberLoading: boolean
  roomsLoading: boolean
  memberError: string
  roomsError: string
  today: string
  maxDate: string
  checkInMax: string
  checkIn: string
  checkOut: string
  checkOutMin: string
  checkInLabel: string
  checkOutLabel: string
  nights: number
}

Page({
  data: {
    member: null,
    rooms: [],
    avatarSource: '../../assets/avatar-default.webp',
    memberLoading: true,
    roomsLoading: true,
    memberError: '',
    roomsError: '',
    today: '',
    maxDate: '',
    checkInMax: '',
    checkIn: '',
    checkOut: '',
    checkOutMin: '',
    checkInLabel: '选择入住日期',
    checkOutLabel: '选择退房日期',
    nights: 0,
  } as HomePageData,

  async onLoad() {
    const { today, lastSelectableDate, lastCheckInDate } = getBookingDateWindow()
    this.setData({
      today,
      maxDate: lastSelectableDate,
      checkInMax: lastCheckInDate,
      checkOutMin: addDays(today, 1),
    })
    await Promise.all([this.loadMember(), this.loadRooms('', '')])
  },

  onShow() {
    const selection = wx.getStorageSync('qx_stay_selection') as { check_in?: string; check_out?: string } | undefined
    if (!selection?.check_in || !selection.check_out) return
    wx.removeStorageSync('qx_stay_selection')
    if (selection.check_in < this.data.today || selection.check_out > this.data.maxDate) return
    this.setData({
      checkIn: selection.check_in,
      checkOut: selection.check_out,
      checkOutMin: addDays(selection.check_in, 1),
      checkInLabel: selection.check_in,
      checkOutLabel: selection.check_out,
      nights: daysBetween(selection.check_in, selection.check_out),
    })
    void this.loadRooms(selection.check_in, selection.check_out)
  },

  async loadMember() {
    this.setData({ memberLoading: true, memberError: '' })
    try {
      const member = await getCurrentMember()
      saveCurrentMember(member)
      this.setData({
        member,
        avatarSource: member.avatar === DEFAULT_AVATAR_PATH ? '../../assets/avatar-default.webp' : member.avatar,
      })
    } catch (error) {
      this.setData({ memberError: error instanceof Error ? error.message : '会员信息读取失败' })
    } finally {
      this.setData({ memberLoading: false })
    }
  },

  async loadRooms(checkIn?: string, checkOut?: string) {
    const selectedCheckIn = checkIn ?? this.data.checkIn
    const selectedCheckOut = checkOut ?? this.data.checkOut
    this.setData({ roomsLoading: true, roomsError: '' })
    try {
      const rooms = await listRooms(selectedCheckIn || undefined, selectedCheckOut || undefined)
      this.setData({ rooms })
    } catch (error) {
      this.setData({ roomsError: error instanceof Error ? error.message : '客房资料读取失败' })
    } finally {
      this.setData({ roomsLoading: false })
    }
  },

  onPullDownRefresh() {
    void Promise.all([this.loadMember(), this.loadRooms()]).finally(() => wx.stopPullDownRefresh())
  },

  onCheckInChange(event: WechatMiniprogram.PickerChange) {
    const checkIn = String(event.detail.value)
    const checkOut = this.data.checkOut > checkIn ? this.data.checkOut : ''
    this.setData({
      checkIn,
      checkOut,
      checkOutMin: addDays(checkIn, 1),
      checkInLabel: checkIn,
      checkOutLabel: checkOut || '选择退房日期',
      nights: checkOut ? daysBetween(checkIn, checkOut) : 0,
    })
  },

  onCheckOutChange(event: WechatMiniprogram.PickerChange) {
    const checkOut = String(event.detail.value)
    this.setData({ checkOut, checkOutLabel: checkOut, nights: daysBetween(this.data.checkIn, checkOut) })
  },

  async onSearchTap() {
    if (!this.data.checkIn || !this.data.checkOut) {
      wx.showToast({ title: '请先选择入住和退房日期', icon: 'none' })
      return
    }
    if (this.data.checkOut <= this.data.checkIn) {
      wx.showToast({ title: '退房日期须晚于入住日期', icon: 'none' })
      return
    }
    await this.loadRooms()
  },

  async onClearDatesTap() {
    this.setData({
      checkIn: '', checkOut: '', checkOutMin: addDays(this.data.today, 1),
      checkInLabel: '选择入住日期', checkOutLabel: '选择退房日期', nights: 0,
    })
    await this.loadRooms('', '')
  },

  onRoomTap(event: WechatMiniprogram.TouchEvent) {
    const roomId = Number(event.currentTarget.dataset.roomId)
    if (!Number.isInteger(roomId) || roomId <= 0) return
    wx.navigateTo({ url: `/pages/room-detail/index?id=${roomId}` })
  },

  onLogoutTap() {
    clearSession()
    wx.reLaunch({ url: '/pages/login/index' })
  },
})
