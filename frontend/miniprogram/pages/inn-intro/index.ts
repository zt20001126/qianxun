/** 芊浔民宿介绍页：展示民宿特色、周边信息和到店指引。 */
import { STORE_ADDRESS, STORE_CONTACT_PHONE, STORE_CONTACT_PHONE_LABEL } from '../../config'

Page({
  data: {
    address: STORE_ADDRESS,
    contactPhone: STORE_CONTACT_PHONE,
    contactPhoneLabel: STORE_CONTACT_PHONE_LABEL,
  },

  onViewRoomsTap() {
    wx.navigateTo({ url: '/pages/rooms/index' })
  },

  onAddressTap() {
    wx.setClipboardData({
      data: this.data.address,
      success: () => wx.showToast({ title: '地址已复制', icon: 'success' }),
    })
  },

  onCallTap() {
    wx.makePhoneCall({ phoneNumber: this.data.contactPhone })
  },
})
