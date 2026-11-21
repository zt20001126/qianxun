import { savePendingInvitation, consumePendingInvitation } from '../../utils/invitation'
import { getValidToken } from '../../utils/session'

Page({
  onLoad(options: Record<string, string | undefined>) {
    savePendingInvitation(options.code || '')
    if (getValidToken()) {
      void consumePendingInvitation()
      wx.switchTab({ url: '/pages/home/index' })
    } else {
      wx.reLaunch({ url: '/pages/login/index' })
    }
  },
})
