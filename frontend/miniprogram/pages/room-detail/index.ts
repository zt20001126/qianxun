/** R02 小程序房间详情。阶段二只浏览资料，日期可订判断在阶段三实现。 */
import { getRoomDetail } from '../../services/rooms'
import type { AppRoomDetail } from '../../types/room'

interface RoomDetailPageData {
  room: AppRoomDetail | null
  loading: boolean
  errorMessage: string
}

Page({
  data: {
    room: null,
    loading: true,
    errorMessage: '',
  } as RoomDetailPageData,

  async onLoad(options: Record<string, string | undefined>) {
    const roomId = Number(options.id)
    if (!Number.isInteger(roomId) || roomId <= 0) {
      this.setData({ loading: false, errorMessage: '房间编号无效' })
      return
    }
    await this.loadRoom(roomId)
  },

  async onPullDownRefresh() {
    const roomId = this.data.room?.id
    if (roomId) await this.loadRoom(roomId)
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

  onBackTap() {
    wx.navigateBack({ delta: 1 })
  },
})
