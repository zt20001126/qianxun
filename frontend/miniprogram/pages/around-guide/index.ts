/** 南浔周边游玩页：展示用户提供的古镇一日游路线图。 */
const GUIDE_IMAGE = '/assets/nanxun-one-day-guide.jpg'

Page({
  data: {
    guideImage: GUIDE_IMAGE,
  },

  /** 打开原图预览，支持微信内置的缩放和拖动。 */
  onPreviewGuide() {
    wx.previewImage({
      current: GUIDE_IMAGE,
      urls: [GUIDE_IMAGE],
    })
  },
})
