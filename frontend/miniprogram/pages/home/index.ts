/** 小程序首页：展示会员简要资料和可浏览客房。 */
import { DEFAULT_AVATAR_PATH } from '../../config'
import { getCurrentMember } from '../../services/auth'
import { listRooms } from '../../services/rooms'
import type { MemberProfile } from '../../types/auth'
import type { AppRoomSummary } from '../../types/room'
import { clearSession, saveCurrentMember } from '../../utils/session'

interface HomePageData {
  member: MemberProfile | null
  rooms: AppRoomSummary[]
  avatarSource: string
  memberLoading: boolean
  roomsLoading: boolean
  memberError: string
  roomsError: string
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
  } as HomePageData,

  async onLoad() {
    await Promise.all([this.loadMember(), this.loadRooms()])
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

  async loadRooms() {
    this.setData({ roomsLoading: true, roomsError: '' })
    try {
      const rooms = await listRooms()
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
