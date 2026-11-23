/** 到店交通页：展示古镇主要入口至芊浔民宿的步行路线参考。 */
import { STORE_ADDRESS } from '../../config'

Page({
  data: {
    address: STORE_ADDRESS,
  },

  /** 复制民宿地址，便于粘贴到地图应用规划实时路线。 */
  onCopyAddress() {
    wx.setClipboardData({
      data: this.data.address,
      success: () => wx.showToast({ title: '民宿地址已复制', icon: 'success' }),
    })
  },
})
